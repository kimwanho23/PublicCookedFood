    document.addEventListener('DOMContentLoaded', function () {
        const policyForm = document.getElementById('boardPolicyForm');
        const thresholdInput = document.getElementById('featuredLikeThreshold');
        const modeSelect = document.getElementById('thumbnailDisplayMode');
        const apiMessage = document.getElementById('adminBoardMessage');
        const tabCreateForm = document.getElementById('boardTabCreateForm');
        const newTabNameInput = document.getElementById('newTabName');
        const tabList = document.getElementById('boardTabList');
        const tabStatus = document.getElementById('boardTabStatus');
        const tabPreview = document.getElementById('boardTabPreview');
        let draggedTabItem = null;
        let dragOrderSnapshot = '';

        if (!policyForm || !thresholdInput || !modeSelect || !apiMessage
            || !tabCreateForm || !newTabNameInput
            || !tabList || !tabStatus || !tabPreview || !window.fetch) {
            return;
        }

        function makeHeaders(includeCsrf) {
            const headers = {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            };
            if (!includeCsrf) {
                return headers;
            }
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }
            return headers;
        }

        function showApiMessage(message, isError) {
            if (!message) {
                apiMessage.classList.add('d-none');
                apiMessage.classList.remove('alert-danger');
                apiMessage.classList.remove('alert-success');
                apiMessage.textContent = '';
                return;
            }
            apiMessage.textContent = message;
            apiMessage.classList.remove('d-none');
            apiMessage.classList.toggle('alert-danger', !!isError);
            apiMessage.classList.toggle('alert-success', !isError);
        }

        async function parseResponse(response, fallbackMessage) {
            const text = await response.text();
            let payload = null;
            if (text) {
                try {
                    payload = JSON.parse(text);
                } catch (ignored) {
                    payload = null;
                }
            }
            if (!response.ok) {
                const message = payload && payload.message ? payload.message : fallbackMessage;
                throw new Error(message);
            }
            return payload || {};
        }

        function markStaticDisabled(control) {
            control.setAttribute('data-static-disabled', 'true');
            applyDisabledState(control, true);
        }

        function applyDisabledState(control, disabled) {
            const isStaticDisabled = control.getAttribute('data-static-disabled') === 'true';
            const nextDisabled = disabled || isStaticDisabled;
            if (control.tagName === 'A') {
                control.classList.toggle('disabled', nextDisabled);
                control.setAttribute('aria-disabled', nextDisabled ? 'true' : 'false');
                control.setAttribute('tabindex', nextDisabled ? '-1' : '0');
                return;
            }
            control.disabled = nextDisabled;
        }

        function setTabRowDisabled(rowElement, disabled) {
            if (!rowElement) {
                return;
            }
            const controls = rowElement.querySelectorAll('input, button, a[data-action]');
            controls.forEach(function (control) {
                applyDisabledState(control, disabled);
            });
        }

        function setTabListDisabled(disabled) {
            const controls = tabList.querySelectorAll('input, button, a[data-action]');
            controls.forEach(function (control) {
                applyDisabledState(control, disabled);
            });
        }

        function getTabItems() {
            return Array.from(tabList.querySelectorAll('li[data-section-id]'));
        }

        function getTabOrderIds() {
            return getTabItems()
                .map(function (item) {
                    return Number.parseInt(item.getAttribute('data-section-id') || '', 10);
                })
                .filter(function (sectionId) {
                    return !Number.isNaN(sectionId);
                });
        }

        function createPreviewPill(tab) {
            const pill = document.createElement('div');
            pill.className = 'board-tab-pill nav-link';
            if (!tab.active) {
                pill.classList.add('is-inactive');
            }
            if (tab.isDefault) {
                pill.classList.add('is-default');
            }

            const name = document.createElement('span');
            name.className = 'board-tab-pill-name';
            name.textContent = tab.name;
            pill.appendChild(name);

            if (tab.isDefault) {
                const badge = document.createElement('span');
                badge.className = 'board-tab-pill-badge is-default';
                badge.textContent = '기본';
                pill.appendChild(badge);
            } else if (!tab.active) {
                const badge = document.createElement('span');
                badge.className = 'board-tab-pill-badge is-inactive';
                badge.textContent = '비활성';
                pill.appendChild(badge);
            }

            return pill;
        }

        function renderTabPreview(tabs) {
            tabPreview.innerHTML = '';
            if (!tabs || tabs.length === 0) {
                const empty = document.createElement('div');
                empty.className = 'board-tab-empty';
                empty.textContent = '아직 등록된 게시판 탭이 없습니다.';
                tabPreview.appendChild(empty);
                return;
            }
            tabs.forEach(function (tab) {
                tabPreview.appendChild(createPreviewPill(tab));
            });
        }

        function syncTabPreviewFromDom() {
            const tabs = getTabItems().map(function (item) {
                const nameInput = item.querySelector('input[data-field="sectionName"]');
                const activeInput = item.querySelector('input[data-field="active"]');
                const isDefault = item.getAttribute('data-default') === 'true';
                return {
                    name: nameInput && nameInput.value.trim() ? nameInput.value.trim() : '이름 없는 탭',
                    key: item.getAttribute('data-section-key') || '',
                    active: isDefault ? true : !!(activeInput && activeInput.checked),
                    isDefault: isDefault
                };
            });
            renderTabPreview(tabs);
        }

        function updateTabOrderIndicators() {
            getTabItems().forEach(function (item, index) {
                const nameInput = item.querySelector('input[data-field="sectionName"]');
                const activeInput = item.querySelector('input[data-field="active"]');
                const title = item.querySelector('[data-tab-title]');
                const meta = item.querySelector('[data-tab-meta]');
                const isDefault = item.getAttribute('data-default') === 'true';
                const tabName = nameInput && nameInput.value.trim() ? nameInput.value.trim() : '이름 없는 탭';
                if (title) {
                    title.textContent = tabName;
                }
                if (meta) {
                    meta.textContent = isDefault
                        ? '기본 탭'
                        : (activeInput && activeInput.checked ? '노출 중' : '비활성');
                }
            });
            syncTabPreviewFromDom();
        }

        function clearDragIndicators() {
            getTabItems().forEach(function (item) {
                item.classList.remove('is-drop-before');
                item.classList.remove('is-drop-after');
                item.classList.remove('is-dragging');
            });
        }

        function renderTabs(tabs) {
            tabList.innerHTML = '';
            if (!tabs || tabs.length === 0) {
                tabStatus.textContent = '등록된 게시판 탭이 없습니다.';
                renderTabPreview([]);
                return;
            }

            const activeCount = tabs.filter(function (tab) {
                return tab.active === true;
            }).length;
            tabStatus.textContent = '총 ' + tabs.length + '개의 게시판, 활성 ' + activeCount + '개';

            tabs.forEach(function (tab) {
                const item = document.createElement('li');
                const sectionId = tab.id != null ? String(tab.id) : '';
                const isDefaultTab = tab.sectionKey === 'general';
                item.className = 'board-tab-item mb-3';
                item.setAttribute('data-section-id', sectionId);
                item.setAttribute('data-section-key', tab.sectionKey ? tab.sectionKey : '');
                item.setAttribute('data-default', String(isDefaultTab));

                const card = document.createElement('article');
                card.className = 'board-tab-card board-tab-handle';
                card.draggable = true;
                card.setAttribute('data-drag-handle', 'true');
                card.title = '카드를 끌어 놓아 탭 순서를 변경하세요.';

                const header = document.createElement('div');
                header.className = 'board-tab-card-header';

                const titleWrap = document.createElement('div');
                titleWrap.className = 'board-tab-order-wrap';

                const grip = document.createElement('span');
                grip.className = 'board-tab-grip';
                grip.setAttribute('aria-hidden', 'true');

                const orderCopy = document.createElement('div');
                orderCopy.className = 'board-tab-order-copy';

                const title = document.createElement('strong');
                title.className = 'board-tab-card-title';
                title.setAttribute('data-tab-title', 'true');
                title.textContent = tab.sectionName ? tab.sectionName : '이름 없는 탭';

                const meta = document.createElement('span');
                meta.className = 'board-tab-card-meta';
                meta.setAttribute('data-tab-meta', 'true');
                meta.textContent = isDefaultTab ? '기본 탭' : (tab.active === true ? '노출 중' : '비활성');

                orderCopy.appendChild(title);
                orderCopy.appendChild(meta);
                titleWrap.appendChild(grip);
                titleWrap.appendChild(orderCopy);

                header.appendChild(titleWrap);

                const bodyRow = document.createElement('div');
                bodyRow.className = 'row g-3 align-items-end';

                const nameCol = document.createElement('div');
                nameCol.className = 'col-lg-6';
                const nameLabel = document.createElement('label');
                nameLabel.className = 'form-label mb-1 small';
                nameLabel.textContent = '탭 이름';
                const nameInput = document.createElement('input');
                nameInput.type = 'text';
                nameInput.className = 'form-control';
                nameInput.maxLength = 100;
                nameInput.value = tab.sectionName ? tab.sectionName : '';
                nameInput.setAttribute('data-field', 'sectionName');
                nameCol.appendChild(nameLabel);
                nameCol.appendChild(nameInput);

                const activeCol = document.createElement('div');
                activeCol.className = 'col-md-4 col-lg-3';
                const activeLabel = document.createElement('label');
                activeLabel.className = 'form-label mb-1 small';
                activeLabel.textContent = '노출 여부';
                const activeWrap = document.createElement('div');
                activeWrap.className = 'form-check form-switch m-0 pt-2';
                const activeInput = document.createElement('input');
                activeInput.type = 'checkbox';
                activeInput.className = 'form-check-input';
                activeInput.setAttribute('data-field', 'active');
                activeInput.checked = tab.active === true;
                if (isDefaultTab) {
                    markStaticDisabled(activeInput);
                }
                const activeText = document.createElement('label');
                activeText.className = 'form-check-label';
                activeText.textContent = isDefaultTab ? '기본 탭은 항상 노출' : '탭 노출';
                activeWrap.appendChild(activeInput);
                activeWrap.appendChild(activeText);
                activeCol.appendChild(activeLabel);
                activeCol.appendChild(activeWrap);

                const actionCol = document.createElement('div');
                actionCol.className = 'col-md-8 col-lg-3';
                const actionRow = document.createElement('div');
                actionRow.className = 'board-tab-inline-actions pt-2';

                const autoSaveNote = document.createElement('span');
                autoSaveNote.className = 'board-tab-autosave-note';
                autoSaveNote.textContent = '변경 사항 자동 저장';

                const deleteLink = document.createElement('a');
                deleteLink.href = '#';
                deleteLink.className = 'link-danger board-tab-delete-link';
                deleteLink.textContent = '삭제';
                deleteLink.setAttribute('data-action', 'delete-tab');
                deleteLink.setAttribute('data-section-id', sectionId);
                if (!sectionId || isDefaultTab) {
                    deleteLink.classList.add('disabled');
                    deleteLink.setAttribute('aria-disabled', 'true');
                    deleteLink.setAttribute('tabindex', '-1');
                }
                if (isDefaultTab) {
                    deleteLink.title = '기본 탭은 삭제할 수 없습니다.';
                }

                actionRow.appendChild(autoSaveNote);
                actionRow.appendChild(deleteLink);
                actionCol.appendChild(actionRow);

                bodyRow.appendChild(nameCol);
                bodyRow.appendChild(activeCol);
                bodyRow.appendChild(actionCol);

                card.appendChild(header);
                card.appendChild(bodyRow);
                item.appendChild(card);
                tabList.appendChild(item);
            });

            updateTabOrderIndicators();
        }

        async function loadTabs() {
            tabStatus.textContent = '게시판 탭 목록을 불러오는 중입니다.';
            try {
                const response = await fetch('/api/admin/boards/sections', {
                    method: 'GET',
                    headers: makeHeaders(false)
                });
                const payload = await parseResponse(response, '게시판 탭 목록을 불러오지 못했습니다.');
                renderTabs(Array.isArray(payload) ? payload : []);
            } catch (error) {
                tabList.innerHTML = '';
                tabStatus.textContent = error && error.message
                    ? error.message
                    : '게시판 탭 목록을 불러오지 못했습니다.';
                renderTabPreview([]);
            }
        }

        async function createTab() {
            const sectionName = newTabNameInput.value ? newTabNameInput.value.trim() : '';
            if (!sectionName) {
                showApiMessage('게시판 탭 이름을 입력해 주세요.', true);
                newTabNameInput.focus();
                return;
            }

            const submitButton = tabCreateForm.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
            }
            showApiMessage('게시판 탭을 추가하는 중입니다...', false);
            try {
                const response = await fetch('/api/admin/boards/sections', {
                    method: 'POST',
                    headers: makeHeaders(true),
                    body: JSON.stringify({
                        sectionName: sectionName
                    })
                });
                await parseResponse(response, '게시판 탭 추가에 실패했습니다.');
                newTabNameInput.value = '';
                showApiMessage('게시판 탭을 추가했습니다.', false);
                await loadTabs();
            } catch (error) {
                const message = error && error.message ? error.message : '게시판 탭 추가에 실패했습니다.';
                showApiMessage(message, true);
            } finally {
                if (submitButton) {
                    submitButton.disabled = false;
                }
            }
        }

        async function updateTab(item, sectionId) {
            const nameInput = item.querySelector('input[data-field="sectionName"]');
            const activeInput = item.querySelector('input[data-field="active"]');
            if (!nameInput || !activeInput) {
                return;
            }

            const sectionName = nameInput.value ? nameInput.value.trim() : '';
            if (!sectionName) {
                showApiMessage('게시판 탭 이름을 입력해 주세요.', true);
                nameInput.focus();
                return;
            }

            setTabRowDisabled(item, true);
            showApiMessage('게시판 탭을 저장하는 중입니다...', false);
            try {
                const response = await fetch('/api/admin/boards/sections/' + encodeURIComponent(sectionId), {
                    method: 'PATCH',
                    headers: makeHeaders(true),
                    body: JSON.stringify({
                        sectionName: sectionName,
                        active: !!activeInput.checked
                    })
                });
                await parseResponse(response, '게시판 탭 저장에 실패했습니다.');
                showApiMessage('게시판 탭을 저장했습니다.', false);
                await loadTabs();
            } catch (error) {
                const message = error && error.message ? error.message : '게시판 탭 저장에 실패했습니다.';
                showApiMessage(message, true);
            } finally {
                if (item.isConnected) {
                    setTabRowDisabled(item, false);
                }
            }
        }

        async function persistTabOrder() {
            const orderedSectionIds = getTabOrderIds();
            if (orderedSectionIds.length === 0) {
                return;
            }
            if (orderedSectionIds.join(',') === dragOrderSnapshot) {
                updateTabOrderIndicators();
                return;
            }

            setTabListDisabled(true);
            showApiMessage('게시판 탭 순서를 저장하는 중입니다...', false);
            try {
                const response = await fetch('/api/admin/boards/sections/reorder', {
                    method: 'PATCH',
                    headers: makeHeaders(true),
                    body: JSON.stringify({sectionIds: orderedSectionIds})
                });
                await parseResponse(response, '게시판 탭 순서 저장에 실패했습니다.');
                showApiMessage('게시판 탭 순서를 저장했습니다.', false);
                await loadTabs();
            } catch (error) {
                const message = error && error.message ? error.message : '게시판 탭 순서 저장에 실패했습니다.';
                showApiMessage(message, true);
                await loadTabs();
            } finally {
                setTabListDisabled(false);
            }
        }

        async function deleteTab(item, sectionId) {
            if (!window.confirm('이 탭을 삭제하면 관련 게시글은 일반 탭으로 이동합니다. 계속하시겠습니까?')) {
                return;
            }
            setTabRowDisabled(item, true);
            showApiMessage('게시판 탭을 삭제하고 게시글을 일반 탭으로 이동하는 중입니다...', false);
            try {
                const response = await fetch('/api/admin/boards/sections/' + encodeURIComponent(sectionId), {
                    method: 'DELETE',
                    headers: makeHeaders(true)
                });
                await parseResponse(response, '게시판 탭 삭제에 실패했습니다.');
                showApiMessage('게시판 탭을 삭제했고 관련 게시글을 일반 탭으로 이동했습니다.', false);
                await loadTabs();
            } catch (error) {
                const message = error && error.message ? error.message : '게시판 탭 삭제에 실패했습니다.';
                showApiMessage(message, true);
            } finally {
                if (item.isConnected) {
                    setTabRowDisabled(item, false);
                }
            }
        }

        policyForm.addEventListener('submit', async function (event) {
            event.preventDefault();

            const featuredLikeThreshold = Number.parseInt(thresholdInput.value, 10);
            const thumbnailDisplayMode = modeSelect.value;
            if (Number.isNaN(featuredLikeThreshold)) {
                showApiMessage('추천글 기준 추천 수를 확인해 주세요.', true);
                thresholdInput.focus();
                return;
            }
            if (!thumbnailDisplayMode) {
                showApiMessage('썸네일 표시 모드를 선택해 주세요.', true);
                modeSelect.focus();
                return;
            }

            showApiMessage('게시판 정책을 저장하는 중입니다...', false);
            const submitButton = policyForm.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
            }

            try {
                const policyResponse = await fetch('/api/admin/boards/policy', {
                    method: 'PATCH',
                    headers: makeHeaders(true),
                    body: JSON.stringify({
                        featuredLikeThreshold: featuredLikeThreshold,
                        thumbnailDisplayMode: thumbnailDisplayMode
                    })
                });
                const policyPayload = await parseResponse(policyResponse, '게시판 정책 저장에 실패했습니다.');

                if (policyPayload && policyPayload.featuredLikeThreshold != null) {
                    thresholdInput.value = String(policyPayload.featuredLikeThreshold);
                }
                if (policyPayload && policyPayload.thumbnailDisplayMode) {
                    modeSelect.value = policyPayload.thumbnailDisplayMode;
                }
                showApiMessage('게시판 정책을 저장했습니다.', false);
            } catch (error) {
                const message = error && error.message ? error.message : '게시판 정책 저장에 실패했습니다.';
                showApiMessage(message, true);
            } finally {
                if (submitButton) {
                    submitButton.disabled = false;
                }
            }
        });

        tabCreateForm.addEventListener('submit', function (event) {
            event.preventDefault();
            void createTab();
        });

        tabList.addEventListener('click', function (event) {
            const actionElement = event.target.closest('[data-action][data-section-id]');
            if (!actionElement) {
                return;
            }
            event.preventDefault();
            const action = actionElement.getAttribute('data-action');
            const sectionId = actionElement.getAttribute('data-section-id');
            const item = actionElement.closest('li[data-section-id]');
            if (!action || !sectionId || !item || actionElement.classList.contains('disabled')) {
                return;
            }
            if (action === 'delete-tab') {
                void deleteTab(item, sectionId);
            }
        });

        tabList.addEventListener('input', function (event) {
            if (event.target.matches('input[data-field="sectionName"]')) {
                updateTabOrderIndicators();
            }
        });

        tabList.addEventListener('change', function (event) {
            if (event.target.matches('input[data-field="sectionName"], input[data-field="active"]')) {
                updateTabOrderIndicators();
                const item = event.target.closest('li[data-section-id]');
                const sectionId = item ? item.getAttribute('data-section-id') : null;
                if (item && sectionId) {
                    void updateTab(item, sectionId);
                }
            }
        });

        tabList.addEventListener('dragstart', function (event) {
            if (event.target.closest('input, textarea, select, option, a, label')) {
                event.preventDefault();
                return;
            }
            const dragHandle = event.target.closest('[data-drag-handle]');
            if (!dragHandle) {
                return;
            }
            draggedTabItem = dragHandle.closest('li[data-section-id]');
            if (!draggedTabItem) {
                return;
            }
            dragOrderSnapshot = getTabOrderIds().join(',');
            draggedTabItem.classList.add('is-dragging');
            if (event.dataTransfer) {
                event.dataTransfer.effectAllowed = 'move';
                event.dataTransfer.setData('text/plain', draggedTabItem.getAttribute('data-section-id') || '');
            }
        });

        tabList.addEventListener('dragover', function (event) {
            if (!draggedTabItem) {
                return;
            }
            const targetItem = event.target.closest('li[data-section-id]');
            if (!targetItem || targetItem === draggedTabItem) {
                return;
            }
            event.preventDefault();
            clearDragIndicators();
            draggedTabItem.classList.add('is-dragging');
            const targetRect = targetItem.getBoundingClientRect();
            const shouldInsertBefore = (event.clientY - targetRect.top) < (targetRect.height / 2);
            targetItem.classList.add(shouldInsertBefore ? 'is-drop-before' : 'is-drop-after');
            tabList.insertBefore(draggedTabItem, shouldInsertBefore ? targetItem : targetItem.nextSibling);
            updateTabOrderIndicators();
        });

        tabList.addEventListener('drop', function (event) {
            if (!draggedTabItem) {
                return;
            }
            event.preventDefault();
            clearDragIndicators();
            const reordered = draggedTabItem;
            draggedTabItem = null;
            if (reordered) {
                void persistTabOrder();
            }
        });

        tabList.addEventListener('dragend', function () {
            clearDragIndicators();
            draggedTabItem = null;
            updateTabOrderIndicators();
        });

        void loadTabs();
    });
