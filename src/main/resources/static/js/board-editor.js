(function (window, document) {
    'use strict';

    var DEFAULT_OPTIONS = {
        editorSelector: '#summernote',
        titleSelector: '#title',
        sectionSelector: '#sectionId',
        formSelector: 'main form',
        draftStorageKey: 'boardDraft:new',
        restoreConfirmMessage: '임시 저장된 작성 내용이 있습니다. 복원할까요?',
        uploadUrl: '/api/images',
        maxImageUploadSizeBytes: 10 * 1024 * 1024
    };

    function init(userOptions) {
        var options = Object.assign({}, DEFAULT_OPTIONS, userOptions || {});
        var editorElement = document.querySelector(options.editorSelector);
        if (!editorElement) {
            return;
        }

        if (editorElement.dataset.boardEditorInitialized === 'true') {
            return;
        }
        editorElement.dataset.boardEditorInitialized = 'true';

        if (typeof window.jQuery !== 'function') {
            console.error('[BoardEditor] jQuery is not loaded.');
            return;
        }
        if (typeof window.jQuery.fn.summernote !== 'function') {
            console.error('[BoardEditor] Summernote plugin is not loaded.');
            return;
        }

        var $ = window.jQuery;
        var currentUploadCount = 0;
        var imagePickerInput = null;
        var activeEditorTarget = null;
        var draftSaveTimer = null;

        function setEditorUploading(uploading) {
            var editor = document.querySelector(options.editorSelector + ' + .note-editor');
            if (!editor) {
                return;
            }
            editor.classList.toggle('is-uploading', !!uploading);
        }

        function handlePasteAsPlainText(event) {
            var clipboardEvent = event && (event.originalEvent || event);
            var clipboard = clipboardEvent && clipboardEvent.clipboardData;
            if (!clipboard || typeof clipboard.getData !== 'function') {
                return;
            }

            var text = clipboard.getData('text/plain');
            if (typeof text !== 'string') {
                return;
            }

            event.preventDefault();
            if (document.queryCommandSupported && document.queryCommandSupported('insertText')) {
                document.execCommand('insertText', false, text);
                return;
            }

            var selection = window.getSelection();
            if (!selection || !selection.rangeCount) {
                return;
            }

            selection.deleteFromDocument();
            selection.getRangeAt(0).insertNode(document.createTextNode(text));
            selection.collapseToEnd();
        }

        function ensureImagePickerInput() {
            if (imagePickerInput) {
                return imagePickerInput;
            }

            imagePickerInput = document.createElement('input');
            imagePickerInput.type = 'file';
            imagePickerInput.accept = 'image/*';
            imagePickerInput.multiple = true;
            imagePickerInput.className = 'd-none';
            imagePickerInput.id = 'summernoteImagePicker';
            document.body.appendChild(imagePickerInput);

            imagePickerInput.addEventListener('change', function () {
                var files = imagePickerInput.files;
                if (!files || files.length === 0 || !activeEditorTarget) {
                    imagePickerInput.value = '';
                    return;
                }

                for (var index = 0; index < files.length; index += 1) {
                    uploadFile(files[index], activeEditorTarget);
                }
                imagePickerInput.value = '';
            });

            return imagePickerInput;
        }

        function openImagePicker(targetEditorElement) {
            if (currentUploadCount > 0) {
                window.alert('이미지 업로드가 진행 중입니다. 잠시만 기다려주세요.');
                return;
            }

            activeEditorTarget = targetEditorElement;
            ensureImagePickerInput().click();
        }

        function bindInsertImageTrigger(targetEditorElement) {
            var editorShell = $(targetEditorElement).next('.note-editor');
            if (!editorShell.length) {
                return;
            }

            editorShell.off('mousedown.boardImageInsert');
            editorShell.on('mousedown.boardImageInsert', '.note-btn[data-event="showImageDialog"]', function (event) {
                event.preventDefault();
            });

            editorShell.off('click.boardImageInsert');
            editorShell.on('click.boardImageInsert', '.note-btn[data-event="showImageDialog"]', function (event) {
                event.preventDefault();
                event.stopImmediatePropagation();
                openImagePicker(targetEditorElement);
            });
        }

        function getDraftPayload(targetEditorElement) {
            var titleInput = document.querySelector(options.titleSelector);
            var sectionInput = document.querySelector(options.sectionSelector);
            var html = $(targetEditorElement).summernote('code');
            return {
                title: titleInput ? titleInput.value : '',
                sectionId: sectionInput ? sectionInput.value : '',
                contents: typeof html === 'string' ? html : '',
                savedAt: new Date().toISOString()
            };
        }

        function hasMeaningfulDraft(payload) {
            if (!payload) {
                return false;
            }
            var text = payload.contents ? payload.contents.replace(/<[^>]*>/g, '').replace(/\s+/g, '') : '';
            return !!((payload.title && payload.title.trim()) || (payload.sectionId && payload.sectionId.trim()) || text);
        }

        function readDraftStorage() {
            if (!window.localStorage) {
                return null;
            }
            try {
                return window.localStorage.getItem(options.draftStorageKey);
            } catch (ignored) {
                return null;
            }
        }

        function saveDraft(targetEditorElement) {
            if (!window.localStorage) {
                return;
            }

            var payload = getDraftPayload(targetEditorElement);
            try {
                if (!hasMeaningfulDraft(payload)) {
                    window.localStorage.removeItem(options.draftStorageKey);
                    return;
                }
                window.localStorage.setItem(options.draftStorageKey, JSON.stringify(payload));
            } catch (ignored) {
            }
        }

        function scheduleDraftSave(targetEditorElement) {
            if (draftSaveTimer) {
                window.clearTimeout(draftSaveTimer);
            }
            draftSaveTimer = window.setTimeout(function () {
                saveDraft(targetEditorElement);
            }, 400);
        }

        function clearDraftStorage() {
            if (!window.localStorage) {
                return;
            }
            try {
                window.localStorage.removeItem(options.draftStorageKey);
            } catch (ignored) {
            }
        }

        function bindDraftEvents(targetEditorElement) {
            var titleInput = document.querySelector(options.titleSelector);
            var sectionInput = document.querySelector(options.sectionSelector);
            var formElement = document.querySelector(options.formSelector);

            if (titleInput) {
                titleInput.addEventListener('input', function () {
                    scheduleDraftSave(targetEditorElement);
                });
            }
            if (sectionInput) {
                sectionInput.addEventListener('change', function () {
                    scheduleDraftSave(targetEditorElement);
                });
            }
            if (formElement) {
                formElement.addEventListener('submit', clearDraftStorage);
            }
        }

        function restoreDraft(targetEditorElement) {
            var raw = readDraftStorage();
            if (!raw) {
                return;
            }

            var draft;
            try {
                draft = JSON.parse(raw);
            } catch (ignored) {
                return;
            }

            if (!hasMeaningfulDraft(draft)) {
                return;
            }

            if (!window.confirm(options.restoreConfirmMessage)) {
                return;
            }

            var titleInput = document.querySelector(options.titleSelector);
            var sectionInput = document.querySelector(options.sectionSelector);

            if (titleInput && typeof draft.title === 'string') {
                titleInput.value = draft.title;
            }
            if (sectionInput && typeof draft.sectionId === 'string' && draft.sectionId) {
                sectionInput.value = draft.sectionId;
            }
            if (typeof draft.contents === 'string') {
                $(targetEditorElement).summernote('code', draft.contents);
            }
        }

        function resolveImageUrl(response) {
            var raw = response;
            if (raw == null) {
                return null;
            }

            if (typeof raw === 'object') {
                if (typeof raw.url === 'string') {
                    raw = raw.url;
                } else if (typeof raw.fileUrl === 'string') {
                    raw = raw.fileUrl;
                } else {
                    return null;
                }
            }

            raw = String(raw).trim();
            if (!raw) {
                return null;
            }

            try {
                var parsed = JSON.parse(raw);
                if (typeof parsed === 'string') {
                    raw = parsed;
                } else if (parsed && typeof parsed.url === 'string') {
                    raw = parsed.url;
                } else if (parsed && typeof parsed.fileUrl === 'string') {
                    raw = parsed.fileUrl;
                }
            } catch (ignored) {
            }

            if (!raw) {
                return null;
            }

            try {
                return new URL(raw, window.location.origin).toString();
            } catch (ignored) {
                return null;
            }
        }

        function resolveUploadErrorMessage(xhr) {
            if (!xhr) {
                return '이미지 업로드에 실패했습니다.';
            }
            if (xhr.status === 401) {
                return '로그인이 필요합니다. 다시 로그인해주세요.';
            }
            if (xhr.status === 403) {
                return '이미지 업로드 권한이 없습니다.';
            }
            if (xhr.status === 413) {
                return '이미지 용량이 너무 큽니다.';
            }
            if (xhr.status >= 500) {
                return '서버 오류로 이미지 업로드에 실패했습니다.';
            }

            var responseText = (xhr.responseText || '').trim();
            if (!responseText) {
                return '이미지 업로드에 실패했습니다.';
            }

            try {
                var parsed = JSON.parse(responseText);
                if (parsed && typeof parsed.message === 'string' && parsed.message.trim() !== '') {
                    return parsed.message;
                }
            } catch (ignored) {
            }
            return responseText;
        }

        function uploadFile(file, targetEditorElement, retryCount) {
            if (!file || !file.type || !file.type.startsWith('image/')) {
                window.alert('이미지 파일만 업로드할 수 있습니다.');
                return;
            }
            if (file.size > options.maxImageUploadSizeBytes) {
                window.alert('이미지는 10MB 이하만 업로드할 수 있습니다.');
                return;
            }

            var attempt = typeof retryCount === 'number' ? retryCount : 0;
            var formData = new FormData();
            formData.append('file', file);

            var csrfToken = $('meta[name="_csrf"]').attr('content');
            var csrfHeader = $('meta[name="_csrf_header"]').attr('content');
            var requestHeaders = {
                'X-Requested-With': 'XMLHttpRequest'
            };
            if (csrfToken && csrfHeader) {
                requestHeaders[csrfHeader] = csrfToken;
            }

            currentUploadCount += 1;
            setEditorUploading(true);

            $.ajax({
                data: formData,
                type: 'POST',
                url: options.uploadUrl,
                headers: requestHeaders,
                dataType: 'text',
                cache: false,
                contentType: false,
                processData: false,
                enctype: 'multipart/form-data',
                success: function (response) {
                    var imageUrl = resolveImageUrl(response);
                    if (!imageUrl) {
                        window.alert('이미지 URL 처리에 실패했습니다.');
                        return;
                    }
                    $(targetEditorElement).summernote('insertImage', imageUrl, function ($image) {
                        $image.attr('loading', 'lazy');
                    });
                },
                error: function (xhr) {
                    var errorMessage = resolveUploadErrorMessage(xhr);
                    if (attempt < 1 && window.confirm(errorMessage + '\n다시 시도할까요?')) {
                        uploadFile(file, targetEditorElement, attempt + 1);
                        return;
                    }
                    window.alert(errorMessage);
                },
                complete: function () {
                    currentUploadCount = Math.max(0, currentUploadCount - 1);
                    setEditorUploading(currentUploadCount > 0);
                }
            });
        }

        $(editorElement).summernote({
            placeholder: '내용을 입력하세요.',
            tabsize: 2,
            height: 520,
            dialogsInBody: true,
            toolbar: [
                ['style', ['style']],
                ['font', ['bold', 'underline', 'clear']],
                ['color', ['color']],
                ['para', ['ul', 'ol', 'paragraph']],
                ['table', ['table']],
                ['insert', ['link', 'picture', 'video']],
                ['view', ['fullscreen', 'codeview']]
            ],
            callbacks: {
                onPaste: function (event) {
                    handlePasteAsPlainText(event);
                },
                onInit: function () {
                    bindInsertImageTrigger(editorElement);
                },
                onChange: function () {
                    scheduleDraftSave(editorElement);
                },
                onImageUpload: function (files) {
                    if (!files || files.length === 0) {
                        return;
                    }
                    if (currentUploadCount > 0) {
                        window.alert('이미지 업로드가 진행 중입니다. 잠시 후 다시 시도해주세요.');
                        return;
                    }
                    for (var index = 0; index < files.length; index += 1) {
                        uploadFile(files[index], this);
                    }
                }
            }
        });

        bindInsertImageTrigger(editorElement);
        bindDraftEvents(editorElement);
        restoreDraft(editorElement);
    }

    window.PubCookBoardEditor = {
        init: init
    };
})(window, document);
