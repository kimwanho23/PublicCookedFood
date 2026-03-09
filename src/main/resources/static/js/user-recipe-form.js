(function () {
    'use strict';

    const MAX_IMAGE_UPLOAD_SIZE_BYTES = 10 * 1024 * 1024;
    const MAX_STEP_COUNT = 15;

    function buildUploadHeaders() {
        const headers = {'X-Requested-With': 'XMLHttpRequest'};
        const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
        const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
        const csrfToken = csrfTokenMeta ? csrfTokenMeta.getAttribute('content') : '';
        const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : '';
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }
        return headers;
    }

    function uploadImage(file) {
        if (!file || !file.type || !file.type.startsWith('image/')) {
            return Promise.reject(new Error('이미지 파일만 업로드할 수 있습니다.'));
        }
        if (file.size > MAX_IMAGE_UPLOAD_SIZE_BYTES) {
            return Promise.reject(new Error('이미지는 10MB 이하만 업로드할 수 있습니다.'));
        }

        const formData = new FormData();
        formData.append('file', file);

        return window.fetch('/api/images/user-recipes', {
            method: 'POST',
            headers: buildUploadHeaders(),
            body: formData,
            credentials: 'same-origin'
        }).then((response) => {
            if (!response.ok) {
                throw new Error('이미지 업로드에 실패했습니다.');
            }
            return response.text();
        }).then((imageUrl) => {
            const normalizedUrl = typeof imageUrl === 'string' ? imageUrl.trim() : '';
            if (!normalizedUrl) {
                throw new Error('이미지 URL 처리에 실패했습니다.');
            }
            return normalizedUrl;
        });
    }

    function updateImagePreview(fieldRoot) {
        if (!fieldRoot) {
            return;
        }
        const urlField = fieldRoot.querySelector('.js-user-recipe-image-url');
        const preview = fieldRoot.querySelector('.js-user-recipe-image-preview');
        if (!urlField || !preview) {
            return;
        }
        const imageUrl = urlField.value ? urlField.value.trim() : '';
        if (!imageUrl) {
            preview.removeAttribute('src');
            preview.style.display = 'none';
            return;
        }
        preview.src = imageUrl;
        preview.style.display = '';
    }

    function setImageStatus(fieldRoot, message, isError) {
        const status = fieldRoot ? fieldRoot.querySelector('.js-user-recipe-image-status') : null;
        if (!status) {
            return;
        }
        status.textContent = message || '';
        status.classList.toggle('text-danger', Boolean(isError));
    }

    function updateIngredientRows(container) {
        const rows = container ? Array.from(container.querySelectorAll('.user-recipe-ingredient-row')) : [];
        rows.forEach((row, index) => {
            row.querySelectorAll('[data-field-name]').forEach((field) => {
                const fieldName = field.getAttribute('data-field-name');
                field.name = 'ingredients[' + index + '].' + fieldName;
                field.id = 'ingredients-' + index + '-' + fieldName;
                const label = row.querySelector('label[for="' + field.htmlFor + '"]');
                if (label) {
                    label.htmlFor = field.id;
                }
            });
        });
    }

    function updateStepRows(container) {
        const rows = container ? Array.from(container.querySelectorAll('.user-recipe-step-row')) : [];
        rows.forEach((row, index) => {
            const stepNumber = index + 1;
            const title = row.querySelector('.user-recipe-step-label');
            if (title) {
                title.textContent = '단계 ' + stepNumber;
            }
            row.querySelectorAll('[data-field-name]').forEach((field) => {
                const fieldName = field.getAttribute('data-field-name');
                field.name = 'steps[' + index + '].' + fieldName;
                field.id = 'steps-' + index + '-' + fieldName;
                if (fieldName === 'stepNo') {
                    field.value = String(stepNumber);
                }
                const label = row.querySelector('label[for="' + field.htmlFor + '"]');
                if (label) {
                    label.htmlFor = field.id;
                }
            });
        });
    }

    function initializeForm() {
        const form = document.querySelector('[data-user-recipe-form]');
        if (!form) {
            return;
        }

        const ingredientRows = document.getElementById('ingredientRows');
        const stepRows = document.getElementById('stepRows');
        const ingredientTemplate = document.getElementById('ingredientRowTemplate');
        const stepTemplate = document.getElementById('stepRowTemplate');

        updateIngredientRows(ingredientRows);
        updateStepRows(stepRows);
        form.querySelectorAll('.js-user-recipe-image-field').forEach(updateImagePreview);

        form.addEventListener('click', function (event) {
            const addButton = event.target.closest('[data-user-recipe-add]');
            if (addButton) {
                const addType = addButton.getAttribute('data-user-recipe-add');
                if (addType === 'ingredient' && ingredientTemplate && ingredientRows) {
                    ingredientRows.appendChild(ingredientTemplate.content.firstElementChild.cloneNode(true));
                    updateIngredientRows(ingredientRows);
                }
                if (addType === 'step' && stepTemplate && stepRows) {
                    const currentSteps = stepRows.querySelectorAll('.user-recipe-step-row').length;
                    if (currentSteps >= MAX_STEP_COUNT) {
                        window.alert('조리 단계는 최대 15단계까지 작성할 수 있습니다.');
                        return;
                    }
                    stepRows.appendChild(stepTemplate.content.firstElementChild.cloneNode(true));
                    updateStepRows(stepRows);
                }
                return;
            }

            const removeButton = event.target.closest('[data-user-recipe-remove]');
            if (removeButton) {
                const removeType = removeButton.getAttribute('data-user-recipe-remove');
                if (removeType === 'ingredient' && ingredientRows) {
                    const rows = ingredientRows.querySelectorAll('.user-recipe-ingredient-row');
                    if (rows.length <= 1) {
                        window.alert('재료는 최소 1개 이상 필요합니다.');
                        return;
                    }
                    removeButton.closest('.user-recipe-ingredient-row').remove();
                    updateIngredientRows(ingredientRows);
                }
                if (removeType === 'step' && stepRows) {
                    const rows = stepRows.querySelectorAll('.user-recipe-step-row');
                    if (rows.length <= 1) {
                        window.alert('조리 단계는 최소 1개 이상 필요합니다.');
                        return;
                    }
                    removeButton.closest('.user-recipe-step-row').remove();
                    updateStepRows(stepRows);
                }
                return;
            }

            const clearButton = event.target.closest('.js-user-recipe-image-clear');
            if (clearButton) {
                const fieldRoot = clearButton.closest('.js-user-recipe-image-field');
                if (!fieldRoot) {
                    return;
                }
                const urlField = fieldRoot.querySelector('.js-user-recipe-image-url');
                const fileInput = fieldRoot.querySelector('.js-user-recipe-image-input');
                if (urlField) {
                    urlField.value = '';
                }
                if (fileInput) {
                    fileInput.value = '';
                }
                setImageStatus(fieldRoot, '', false);
                updateImagePreview(fieldRoot);
            }
        });

        form.addEventListener('change', function (event) {
            const imageInput = event.target.closest('.js-user-recipe-image-input');
            if (!imageInput) {
                return;
            }
            const fieldRoot = imageInput.closest('.js-user-recipe-image-field');
            if (!fieldRoot || !imageInput.files || imageInput.files.length === 0) {
                return;
            }
            const [file] = imageInput.files;
            setImageStatus(fieldRoot, '이미지 업로드 중...', false);
            uploadImage(file)
                .then((imageUrl) => {
                    const urlField = fieldRoot.querySelector('.js-user-recipe-image-url');
                    if (urlField) {
                        urlField.value = imageUrl;
                    }
                    setImageStatus(fieldRoot, '이미지가 업로드되었습니다.', false);
                    updateImagePreview(fieldRoot);
                })
                .catch((error) => {
                    setImageStatus(fieldRoot, error.message || '이미지 업로드에 실패했습니다.', true);
                    window.alert(error.message || '이미지 업로드에 실패했습니다.');
                })
                .finally(() => {
                    imageInput.value = '';
                });
        });
    }

    function initializeStandaloneUploads() {
        document.querySelectorAll('.js-user-recipe-image-field').forEach(updateImagePreview);
        document.addEventListener('click', function (event) {
            const clearButton = event.target.closest('.js-user-recipe-image-clear');
            if (!clearButton || clearButton.closest('[data-user-recipe-form]')) {
                return;
            }
            const fieldRoot = clearButton.closest('.js-user-recipe-image-field');
            if (!fieldRoot) {
                return;
            }
            const urlField = fieldRoot.querySelector('.js-user-recipe-image-url');
            const fileInput = fieldRoot.querySelector('.js-user-recipe-image-input');
            if (urlField) {
                urlField.value = '';
            }
            if (fileInput) {
                fileInput.value = '';
            }
            setImageStatus(fieldRoot, '', false);
            updateImagePreview(fieldRoot);
        });

        document.addEventListener('change', function (event) {
            const imageInput = event.target.closest('.js-user-recipe-image-input');
            if (!imageInput || imageInput.closest('[data-user-recipe-form]')) {
                return;
            }
            const fieldRoot = imageInput.closest('.js-user-recipe-image-field');
            if (!fieldRoot || !imageInput.files || imageInput.files.length === 0) {
                return;
            }
            const [file] = imageInput.files;
            setImageStatus(fieldRoot, '이미지 업로드 중...', false);
            uploadImage(file)
                .then((imageUrl) => {
                    const urlField = fieldRoot.querySelector('.js-user-recipe-image-url');
                    if (urlField) {
                        urlField.value = imageUrl;
                    }
                    setImageStatus(fieldRoot, '이미지가 업로드되었습니다.', false);
                    updateImagePreview(fieldRoot);
                })
                .catch((error) => {
                    setImageStatus(fieldRoot, error.message || '이미지 업로드에 실패했습니다.', true);
                    window.alert(error.message || '이미지 업로드에 실패했습니다.');
                })
                .finally(() => {
                    imageInput.value = '';
                });
        });
    }

    function initializeAsyncForms(root) {
        const searchRoot = root || document;
        searchRoot.querySelectorAll('[data-user-recipe-async-form]').forEach((form) => {
            if (form.dataset.asyncBound === 'true') {
                return;
            }
            form.dataset.asyncBound = 'true';

            form.addEventListener('submit', function (event) {
                event.preventDefault();
                if (form.dataset.submitting === 'true') {
                    return;
                }
                const submitter = event.submitter || null;
                const confirmMessage = (submitter && submitter.dataset && submitter.dataset.userRecipeConfirm)
                    || form.getAttribute('data-user-recipe-confirm');
                if (confirmMessage && !window.confirm(confirmMessage)) {
                    return;
                }

                const submitButtons = Array.from(form.querySelectorAll('button[type="submit"]'));
                const requestUrl = submitter && submitter.formAction ? submitter.formAction : form.action;
                const requestMethod = submitter && submitter.getAttribute('formmethod')
                    ? submitter.getAttribute('formmethod')
                    : (form.method || 'post');
                const requestBody = submitter ? new FormData(form, submitter) : new FormData(form);
                form.dataset.submitting = 'true';
                submitButtons.forEach((button) => {
                    button.disabled = true;
                });

                window.fetch(requestUrl, {
                    method: requestMethod.toUpperCase(),
                    headers: buildUploadHeaders(),
                    body: requestBody,
                    credentials: 'same-origin',
                    redirect: 'follow'
                }).then(async (response) => {
                    if (!response.ok) {
                        throw new Error('요청 처리에 실패했습니다.');
                    }

                    const html = await response.text();
                    const targetSelector = form.getAttribute('data-user-recipe-async-target');
                    const parser = new window.DOMParser();
                    const nextDocument = parser.parseFromString(html, 'text/html');
                    const nextTarget = targetSelector ? nextDocument.querySelector(targetSelector) : null;
                    const currentTarget = targetSelector ? document.querySelector(targetSelector) : null;
                    const nextAlerts = nextDocument.querySelector('#userRecipeAlerts');
                    const currentAlerts = document.querySelector('#userRecipeAlerts');

                    if (!nextTarget || !currentTarget) {
                        if (response.url) {
                            window.location.assign(response.url);
                            return;
                        }
                        window.location.reload();
                        return;
                    }

                    currentTarget.replaceWith(nextTarget);
                    if (nextAlerts && currentAlerts) {
                        currentAlerts.replaceWith(nextAlerts);
                    }
                    if (response.url) {
                        window.history.replaceState({}, '', response.url);
                    }
                    nextTarget.querySelectorAll('.js-user-recipe-image-field').forEach(updateImagePreview);
                    initializeAsyncForms(nextTarget);
                }).catch((error) => {
                    submitButtons.forEach((button) => {
                        button.disabled = false;
                    });
                    form.dataset.submitting = 'false';
                    window.alert(error.message || '요청 처리에 실패했습니다.');
                });
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        initializeForm();
        initializeStandaloneUploads();
        initializeAsyncForms(document);
    });
})();
