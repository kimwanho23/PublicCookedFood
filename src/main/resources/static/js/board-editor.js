(function (window, document) {
    'use strict';

    var DEFAULT_OPTIONS = {
        editorContainerSelector: '#boardEditor',
        hiddenInputSelector: '#boardContents',
        titleSelector: '#title',
        sectionSelector: '#sectionId',
        formSelector: 'main form',
        manualDraftSaveButtonSelector: '.js-board-draft-save',
        draftStatusSelector: '.js-board-draft-status',
        draftStorageKey: 'boardDraft:new',
        restoreConfirmMessage: '임시 저장된 작성 내용이 있습니다. 복원할까요?',
        uploadUrl: '/api/images',
        maxImageUploadSizeBytes: 10 * 1024 * 1024,
        defaultTableRows: 2,
        defaultTableColumns: 2,
        minTableRows: 1,
        maxTableRows: 10,
        minTableColumns: 1,
        maxTableColumns: 10
    };

    function init(userOptions) {
        var options = Object.assign({}, DEFAULT_OPTIONS, userOptions || {});
        var editorContainer = document.querySelector(options.editorContainerSelector);
        var hiddenInput = document.querySelector(options.hiddenInputSelector);
        if (!editorContainer || !hiddenInput) {
            return;
        }

        if (editorContainer.dataset.boardEditorInitialized === 'true') {
            return;
        }
        editorContainer.dataset.boardEditorInitialized = 'true';

        if (typeof window.Quill !== 'function') {
            console.error('[BoardEditor] Quill is not loaded.');
            return;
        }

        configureCustomIcons();

        var currentUploadCount = 0;
        var imagePickerInput = null;
        var draftSaveTimer = null;
        var isCodeView = false;
        var isFullscreen = false;
        var tablePickerState = null;
        var toolbarContainer = null;
        var draftStatusElement = null;
        var pendingPlaceholderCleanup = false;

        var quill = new window.Quill(editorContainer, {
            theme: 'snow',
            placeholder: '내용을 입력하세요.',
            modules: {
                toolbar: {
                    container: [
                        [{ header: [1, 2, 3, false] }],
                        ['bold', 'italic', 'underline', 'strike'],
                        [{ color: [] }, { background: [] }],
                        [{ list: 'ordered' }, { list: 'bullet' }],
                        ['blockquote', 'code-block', 'link', 'image', 'video', 'table'],
                        ['clean', 'codeview', 'fullscreen']
                    ],
                    handlers: {
                        image: function () {
                            openImagePicker();
                        },
                        video: function () {
                            openVideoPrompt();
                        },
                        table: function () {
                            insertDefaultTable(toolbarContainer ? toolbarContainer.querySelector('.ql-table') : null);
                        },
                        clean: function () {
                            handleClearFormatting();
                        },
                        codeview: function () {
                            toggleCodeView();
                        },
                        fullscreen: function () {
                            toggleFullscreen();
                        }
                    }
                },
                history: {
                    delay: 400,
                    maxStack: 100,
                    userOnly: true
                }
            }
        });

        var toolbarModule = quill.getModule('toolbar');
        toolbarContainer = toolbarModule && toolbarModule.container ? toolbarModule.container : null;
        if (toolbarContainer) {
            toolbarContainer.classList.add('board-quill-toolbar');
            if (editorContainer.classList.contains('board-quill-shell-invalid')) {
                toolbarContainer.classList.add('board-quill-toolbar-invalid');
            }
        }

        var codeViewTextarea = document.createElement('textarea');
        codeViewTextarea.className = 'board-quill-codeview';
        codeViewTextarea.setAttribute('spellcheck', 'false');
        codeViewTextarea.setAttribute('autocomplete', 'off');
        codeViewTextarea.setAttribute('autocorrect', 'off');
        codeViewTextarea.setAttribute('autocapitalize', 'off');
        editorContainer.appendChild(codeViewTextarea);

        quill.root.setAttribute('spellcheck', 'false');
        quill.root.setAttribute('autocomplete', 'off');
        quill.root.setAttribute('autocorrect', 'off');
        quill.root.setAttribute('autocapitalize', 'off');

        setEditorContents(hiddenInput.value);
        syncHiddenInput();
        updateToolbarToggleStates();
        applyToolbarButtonTitles();

        function configureCustomIcons() {
            if (typeof window.Quill.import !== 'function') {
                return;
            }
            try {
                var icons = window.Quill.import('ui/icons');
                icons.clean = '<svg viewBox="0 0 18 18"><line class="ql-stroke" x1="4.5" y1="4.5" x2="13.5" y2="13.5"></line><line class="ql-stroke" x1="13.5" y1="4.5" x2="4.5" y2="13.5"></line><path class="ql-stroke" d="M2.5,9h13"></path></svg>';
                icons.table = '<svg viewBox="0 0 18 18"><rect class="ql-stroke" x="2.5" y="2.5" width="13" height="13"></rect><line class="ql-stroke" x1="2.5" y1="7" x2="15.5" y2="7"></line><line class="ql-stroke" x1="2.5" y1="11" x2="15.5" y2="11"></line><line class="ql-stroke" x1="7" y1="2.5" x2="7" y2="15.5"></line><line class="ql-stroke" x1="11" y1="2.5" x2="11" y2="15.5"></line></svg>';
                icons.codeview = '<svg viewBox="0 0 18 18"><polyline class="ql-stroke" points="7 5 3.5 9 7 13"></polyline><polyline class="ql-stroke" points="11 5 14.5 9 11 13"></polyline></svg>';
                icons.fullscreen = '<svg viewBox="0 0 18 18"><polyline class="ql-stroke" points="7 2.5 2.5 2.5 2.5 7"></polyline><polyline class="ql-stroke" points="11 2.5 15.5 2.5 15.5 7"></polyline><polyline class="ql-stroke" points="15.5 11 15.5 15.5 11 15.5"></polyline><polyline class="ql-stroke" points="7 15.5 2.5 15.5 2.5 11"></polyline></svg>';
            } catch (ignored) {
            }
        }

        function applyToolbarButtonTitles() {
            var toolbar = toolbarContainer;
            if (!toolbar) {
                return;
            }

            function setButtonTitle(selector, title) {
                var button = toolbar.querySelector(selector);
                if (!button) {
                    return;
                }
                button.setAttribute('title', title);
                button.setAttribute('aria-label', title);
            }

            setButtonTitle('.ql-clean', '서식 제거');
            setButtonTitle('.ql-table', '표 삽입');
            setButtonTitle('.ql-codeview', 'HTML 보기');
            setButtonTitle('.ql-fullscreen', '전체화면');
        }

        function getDraftStatusElement() {
            if (draftStatusElement) {
                return draftStatusElement;
            }
            draftStatusElement = document.querySelector(options.draftStatusSelector);
            return draftStatusElement;
        }

        function setDraftStatusMessage(message) {
            var statusElement = getDraftStatusElement();
            if (!statusElement) {
                return;
            }
            statusElement.textContent = message || '';
        }

        function formatSavedClock(isoDateTime) {
            if (typeof isoDateTime !== 'string') {
                return '';
            }
            var date = new Date(isoDateTime);
            if (Number.isNaN(date.getTime())) {
                return '';
            }
            var hour = String(date.getHours()).padStart(2, '0');
            var minute = String(date.getMinutes()).padStart(2, '0');
            var second = String(date.getSeconds()).padStart(2, '0');
            return hour + ':' + minute + ':' + second;
        }

        function normalizeEditorHtml(html) {
            if (typeof html !== 'string') {
                return '';
            }
            var normalized = html.trim();
            if (!normalized || normalized === '<p><br></p>') {
                return '';
            }
            var normalizedRoot = document.createElement('div');
            normalizedRoot.innerHTML = normalized;
            if (isSinglePlaceholderTableDocument(normalizedRoot)) {
                return '';
            }
            return normalized;
        }

        function normalizeVideoUrl(rawUrl) {
            if (typeof rawUrl !== 'string') {
                return null;
            }

            var candidate = rawUrl.trim();
            if (!candidate) {
                return null;
            }

            var parsed;
            try {
                parsed = new URL(candidate, window.location.origin);
            } catch (ignored) {
                return null;
            }

            if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
                return null;
            }

            var hostname = parsed.hostname.toLowerCase();
            if (hostname === 'youtu.be') {
                var shortId = parsed.pathname.replace(/^\/+/, '');
                if (!shortId) {
                    return null;
                }
                return 'https://www.youtube.com/embed/' + encodeURIComponent(shortId);
            }

            if (hostname.endsWith('youtube.com')) {
                if (parsed.pathname === '/watch') {
                    var watchId = parsed.searchParams.get('v');
                    if (watchId) {
                        return 'https://www.youtube.com/embed/' + encodeURIComponent(watchId);
                    }
                }
                if (parsed.pathname.indexOf('/shorts/') === 0) {
                    var shortPathId = parsed.pathname.substring('/shorts/'.length);
                    if (shortPathId) {
                        return 'https://www.youtube.com/embed/' + encodeURIComponent(shortPathId);
                    }
                }
                if (parsed.pathname.indexOf('/embed/') === 0) {
                    return parsed.toString();
                }
            }

            if (hostname.endsWith('vimeo.com')) {
                var pathMatch = parsed.pathname.match(/\/(\d+)(?:\/|$)/);
                if (pathMatch) {
                    return 'https://player.vimeo.com/video/' + pathMatch[1];
                }
            }

            return parsed.toString();
        }

        function setEditorUploading(uploading) {
            editorContainer.classList.toggle('is-uploading', !!uploading);
            if (toolbarContainer) {
                toolbarContainer.classList.toggle('is-uploading', !!uploading);
            }
        }

        function extractTextFromHtml(html) {
            if (!html) {
                return '';
            }
            var temp = document.createElement('div');
            temp.innerHTML = html;
            return (temp.textContent || '').replace(/\s+/g, '');
        }

        function hasMeaningfulDomContent(element) {
            if (!element) {
                return false;
            }
            var text = (element.textContent || '').replace(/\s+/g, '');
            if (text) {
                return true;
            }
            return !!element.querySelector('img,video,iframe,embed,object,canvas,svg,input,textarea,select,hr');
        }

        function readEditorHtml() {
            if (isCodeView) {
                return normalizeEditorHtml(codeViewTextarea.value);
            }
            return normalizeEditorHtml(quill.root.innerHTML);
        }

        function syncHiddenInput() {
            hiddenInput.value = readEditorHtml();
        }

        function setEditorContents(html) {
            var normalized = normalizeEditorHtml(html);
            if (!normalized) {
                quill.setText('', 'silent');
                codeViewTextarea.value = '';
                return;
            }

            quill.setText('', 'silent');
            quill.clipboard.dangerouslyPasteHTML(0, normalized, 'silent');
            quill.history.clear();
            codeViewTextarea.value = normalized;
        }

        function getEditorContentLength() {
            return Math.max(0, quill.getLength() - 1);
        }

        function isWholeDocumentSelection(range) {
            if (!range) {
                return false;
            }
            var contentLength = getEditorContentLength();
            if (contentLength <= 0) {
                return false;
            }
            return range.index <= 0 && range.length >= contentLength;
        }

        function clearEntireEditor() {
            closeTablePicker();
            setEditorContents('');
            quill.setSelection(0, 0, 'silent');
            syncHiddenInput();
            scheduleDraftSave();
        }

        function updateToolbarToggleStates() {
            var codeViewButton = toolbarContainer ? toolbarContainer.querySelector('.ql-codeview') : null;
            var fullscreenButton = toolbarContainer ? toolbarContainer.querySelector('.ql-fullscreen') : null;
            if (codeViewButton) {
                codeViewButton.classList.toggle('ql-active', isCodeView);
            }
            if (fullscreenButton) {
                fullscreenButton.classList.toggle('ql-active', isFullscreen);
            }
        }

        function updateViewportHeightVariable() {
            document.documentElement.style.setProperty('--board-editor-vh', window.innerHeight + 'px');
        }

        function toggleFullscreen(force) {
            var nextState = typeof force === 'boolean' ? force : !isFullscreen;
            if (nextState === isFullscreen) {
                return;
            }
            closeTablePicker();

            isFullscreen = nextState;
            editorContainer.classList.toggle('is-fullscreen', isFullscreen);
            if (toolbarContainer) {
                toolbarContainer.classList.toggle('is-fullscreen', isFullscreen);
            }
            document.body.classList.toggle('board-editor-fullscreen-lock', isFullscreen);
            if (isFullscreen) {
                updateViewportHeightVariable();
                window.addEventListener('resize', updateViewportHeightVariable);
            } else {
                window.removeEventListener('resize', updateViewportHeightVariable);
            }
            updateToolbarToggleStates();
        }

        function toggleCodeView(force) {
            var nextState = typeof force === 'boolean' ? force : !isCodeView;
            if (nextState === isCodeView) {
                return;
            }
            closeTablePicker();

            if (nextState) {
                syncHiddenInput();
                codeViewTextarea.value = hiddenInput.value;
                editorContainer.classList.add('is-codeview');
                if (toolbarContainer) {
                    toolbarContainer.classList.add('is-codeview');
                }
                quill.enable(false);
                codeViewTextarea.focus();
            } else {
                setEditorContents(codeViewTextarea.value);
                editorContainer.classList.remove('is-codeview');
                if (toolbarContainer) {
                    toolbarContainer.classList.remove('is-codeview');
                }
                quill.enable(true);
                quill.focus();
            }

            isCodeView = nextState;
            syncHiddenInput();
            scheduleDraftSave();
            updateToolbarToggleStates();
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
            imagePickerInput.id = 'boardEditorImagePicker';
            document.body.appendChild(imagePickerInput);

            imagePickerInput.addEventListener('change', function () {
                var files = imagePickerInput.files;
                if (!files || files.length === 0) {
                    imagePickerInput.value = '';
                    return;
                }

                if (currentUploadCount > 0) {
                    window.alert('이미지 업로드가 진행 중입니다. 잠시 후 다시 시도해주세요.');
                    imagePickerInput.value = '';
                    return;
                }

                for (var index = 0; index < files.length; index += 1) {
                    uploadFile(files[index]);
                }
                imagePickerInput.value = '';
            });

            return imagePickerInput;
        }

        function openImagePicker() {
            if (isCodeView) {
                window.alert('코드뷰에서는 이미지 버튼을 사용할 수 없습니다. 코드뷰를 종료해주세요.');
                return;
            }
            if (currentUploadCount > 0) {
                window.alert('이미지 업로드가 진행 중입니다. 잠시만 기다려주세요.');
                return;
            }

            ensureImagePickerInput().click();
        }

        function openVideoPrompt() {
            if (isCodeView) {
                window.alert('코드뷰에서는 비디오 버튼을 사용할 수 없습니다. 코드뷰를 종료해주세요.');
                return;
            }

            var input = window.prompt('영상 URL을 입력하세요.');
            if (input == null) {
                return;
            }

            var normalizedVideoUrl = normalizeVideoUrl(input);
            if (!normalizedVideoUrl) {
                window.alert('올바른 영상 URL을 입력해주세요.');
                return;
            }

            insertVideoAtCursor(normalizedVideoUrl);
        }

        function insertVideoAtCursor(videoUrl) {
            var range = quill.getSelection(true);
            var index = range ? range.index : quill.getLength();
            quill.insertEmbed(index, 'video', videoUrl, 'user');
            quill.setSelection(index + 1, 0, 'silent');
            scheduleDraftSave();
        }

        function getTableElementAtIndex(index) {
            if (!Number.isInteger(index) || index < 0) {
                return null;
            }

            var leafData = quill.getLeaf(index);
            if (!leafData || !leafData[0] || !leafData[0].domNode) {
                return null;
            }

            var leafNode = leafData[0].domNode;
            if (!leafNode || typeof leafNode.closest !== 'function') {
                return null;
            }

            return leafNode.closest('table');
        }

        function getActiveTableElement() {
            var selection = quill.getSelection(true);
            if (!selection) {
                return null;
            }

            var direct = getTableElementAtIndex(selection.index);
            if (direct) {
                return direct;
            }

            if (selection.length > 0) {
                var rangeEndIndex = selection.index + Math.max(0, selection.length - 1);
                var atRangeEnd = getTableElementAtIndex(rangeEndIndex);
                if (atRangeEnd) {
                    return atRangeEnd;
                }
            }

            var nativeSelection = window.getSelection();
            if (nativeSelection && nativeSelection.rangeCount > 0) {
                var anchor = nativeSelection.anchorNode;
                var focus = nativeSelection.focusNode;
                var anchorElement = anchor && anchor.nodeType === 1 ? anchor : (anchor ? anchor.parentElement : null);
                var focusElement = focus && focus.nodeType === 1 ? focus : (focus ? focus.parentElement : null);

                if (anchorElement && typeof anchorElement.closest === 'function') {
                    var fromAnchor = anchorElement.closest('table');
                    if (fromAnchor) {
                        return fromAnchor;
                    }
                }
                if (focusElement && typeof focusElement.closest === 'function') {
                    var fromFocus = focusElement.closest('table');
                    if (fromFocus) {
                        return fromFocus;
                    }
                }
            }

            if (nativeSelection && nativeSelection.rangeCount > 0) {
                var nativeRange = nativeSelection.getRangeAt(0);
                var allTables = quill.root.querySelectorAll('table');
                for (var idx = 0; idx < allTables.length; idx += 1) {
                    var candidateTable = allTables[idx];
                    try {
                        if (typeof nativeRange.intersectsNode === 'function' && nativeRange.intersectsNode(candidateTable)) {
                            return candidateTable;
                        }
                    } catch (ignored) {
                    }
                }
            }

            return null;
        }

        function getTableElementsOverlappingRange(range) {
            if (!range || typeof quill.getIndex !== 'function') {
                return [];
            }

            var selectionStart = range.index;
            var selectionEnd = range.index + Math.max(1, range.length);
            var tables = quill.root.querySelectorAll('table');
            var matched = [];

            for (var index = 0; index < tables.length; index += 1) {
                var tableElement = tables[index];
                var blot;
                try {
                    blot = window.Quill.find(tableElement);
                } catch (ignored) {
                    blot = null;
                }

                if (!blot || typeof blot.length !== 'function') {
                    continue;
                }

                var tableStart;
                try {
                    tableStart = quill.getIndex(blot);
                } catch (ignored) {
                    continue;
                }

                var tableLength = Math.max(1, blot.length());
                var tableEnd = tableStart + tableLength;
                var overlaps = selectionStart < tableEnd && selectionEnd > tableStart;
                if (overlaps) {
                    matched.push(tableElement);
                }
            }

            return matched;
        }

        function isSingleCellPlaceholderTableElement(tableElement) {
            if (!tableElement) {
                return false;
            }

            var rows = tableElement.querySelectorAll('tr');
            if (rows.length !== 1) {
                return false;
            }

            var cells = rows[0].querySelectorAll('td,th');
            if (cells.length !== 1) {
                return false;
            }

            return !hasMeaningfulDomContent(cells[0]);
        }

        function isSinglePlaceholderTableDocument(containerElement) {
            if (!containerElement) {
                return false;
            }

            var tables = containerElement.querySelectorAll('table');
            if (tables.length !== 1) {
                return false;
            }

            if (!isSingleCellPlaceholderTableElement(tables[0])) {
                return false;
            }

            var withoutTable = containerElement.cloneNode(true);
            var copyTable = withoutTable.querySelector('table');
            if (copyTable) {
                copyTable.remove();
            }
            return !hasMeaningfulDomContent(withoutTable);
        }

        function cleanupSinglePlaceholderTable() {
            var rootCopy = document.createElement('div');
            rootCopy.innerHTML = quill.root.innerHTML;
            if (!isSinglePlaceholderTableDocument(rootCopy)) {
                return false;
            }

            clearEntireEditor();
            return true;
        }

        function removeTableElements(tableElements) {
            if (!tableElements || tableElements.length === 0) {
                return false;
            }

            var markerAttribute = 'data-board-table-remove-target';
            for (var markerIndex = 0; markerIndex < tableElements.length; markerIndex += 1) {
                var tableElement = tableElements[markerIndex];
                if (tableElement) {
                    tableElement.setAttribute(markerAttribute, '1');
                }
            }

            var rootCopy = document.createElement('div');
            rootCopy.innerHTML = quill.root.innerHTML;

            var targetTables = rootCopy.querySelectorAll('table[' + markerAttribute + '="1"]');
            if (!targetTables || targetTables.length === 0) {
                var fallbackTable = rootCopy.querySelector('table');
                if (fallbackTable) {
                    targetTables = [fallbackTable];
                }
            }
            if (!targetTables || targetTables.length === 0) {
                for (var clearIndex = 0; clearIndex < tableElements.length; clearIndex += 1) {
                    if (tableElements[clearIndex]) {
                        tableElements[clearIndex].removeAttribute(markerAttribute);
                    }
                }
                return false;
            }

            for (var replaceIndex = 0; replaceIndex < targetTables.length; replaceIndex += 1) {
                var targetTable = targetTables[replaceIndex];
                var replacementParagraph = document.createElement('p');
                replacementParagraph.appendChild(document.createElement('br'));
                targetTable.replaceWith(replacementParagraph);
            }
            for (var cleanupIndex = 0; cleanupIndex < tableElements.length; cleanupIndex += 1) {
                if (tableElements[cleanupIndex]) {
                    tableElements[cleanupIndex].removeAttribute(markerAttribute);
                }
            }

            var nextHtml = rootCopy.innerHTML;
            if (!nextHtml || nextHtml.trim() === '') {
                nextHtml = '<p><br></p>';
            }
            setEditorContents(nextHtml);
            syncHiddenInput();
            scheduleDraftSave();
            return true;
        }

        function removeActiveTable() {
            return removeTableElements([getActiveTableElement()]);
        }

        function handleClearFormatting() {
            closeTablePicker();

            var range = quill.getSelection(true);
            if (!range) {
                return;
            }
            pendingPlaceholderCleanup = true;

            if (isWholeDocumentSelection(range)) {
                pendingPlaceholderCleanup = false;
                clearEntireEditor();
                return;
            }

            var selectedTables = getTableElementsOverlappingRange(range);
            if (selectedTables.length > 0 && removeTableElements(selectedTables)) {
                return;
            }
            if (removeActiveTable()) {
                return;
            }

            if (range.length > 0) {
                quill.removeFormat(range.index, range.length, 'user');
                scheduleDraftSave();
                return;
            }

            var lineData = quill.getLine(range.index);
            var line = lineData ? lineData[0] : null;
            if (!line || typeof quill.getIndex !== 'function') {
                return;
            }

            var lineStart = quill.getIndex(line);
            var lineLength = Math.max(1, line.length() - 1);
            quill.removeFormat(lineStart, lineLength, 'user');
            scheduleDraftSave();
        }

        function handleEditorKeydown(event) {
            if (!event || (event.key !== 'Backspace' && event.key !== 'Delete')) {
                return;
            }

            pendingPlaceholderCleanup = true;
            var range = quill.getSelection();
            if (!range) {
                pendingPlaceholderCleanup = false;
                return;
            }
            if (!isWholeDocumentSelection(range)) {
                return;
            }

            event.preventDefault();
            pendingPlaceholderCleanup = false;
            clearEntireEditor();
        }

        function normalizeTableSize(value, min, max, fallback) {
            var parsed = Number(value);
            if (!Number.isInteger(parsed)) {
                parsed = Number(fallback);
            }
            if (!Number.isInteger(parsed)) {
                parsed = min;
            }
            return Math.max(min, Math.min(max, parsed));
        }

        function ensureTablePicker() {
            if (tablePickerState) {
                return tablePickerState;
            }

            var pickerRoot = document.createElement('div');
            pickerRoot.className = 'board-table-picker';
            pickerRoot.hidden = true;
            pickerRoot.setAttribute('aria-hidden', 'true');

            var pickerSummary = document.createElement('div');
            pickerSummary.className = 'board-table-picker__summary';
            pickerSummary.textContent = '표 크기 선택';

            var pickerGrid = document.createElement('div');
            pickerGrid.className = 'board-table-picker__grid';

            pickerRoot.appendChild(pickerSummary);
            pickerRoot.appendChild(pickerGrid);
            document.body.appendChild(pickerRoot);

            tablePickerState = {
                root: pickerRoot,
                summary: pickerSummary,
                grid: pickerGrid,
                cells: [],
                button: null,
                isOpen: false,
                minRows: 1,
                maxRows: 10,
                minColumns: 1,
                maxColumns: 10,
                outsideClickHandler: null,
                keydownHandler: null,
                resizeHandler: null
            };

            pickerGrid.addEventListener('mousemove', function (event) {
                var cell = event.target.closest('.board-table-picker__cell');
                if (!cell || !tablePickerState || !tablePickerState.isOpen) {
                    return;
                }
                updateTablePickerSelection(Number(cell.dataset.row), Number(cell.dataset.column));
            });

            pickerGrid.addEventListener('mouseleave', function () {
                if (!tablePickerState || !tablePickerState.isOpen) {
                    return;
                }
                updateTablePickerSelection(0, 0);
            });

            pickerGrid.addEventListener('click', function (event) {
                var cell = event.target.closest('.board-table-picker__cell');
                if (!cell || !tablePickerState || !tablePickerState.isOpen) {
                    return;
                }
                event.preventDefault();

                var selectedRows = Math.max(tablePickerState.minRows, Number(cell.dataset.row));
                var selectedColumns = Math.max(tablePickerState.minColumns, Number(cell.dataset.column));
                closeTablePicker();
                insertTableWithSize(selectedRows, selectedColumns);
            });

            return tablePickerState;
        }

        function renderTablePickerCells(maxRows, maxColumns) {
            var picker = ensureTablePicker();
            if (!picker) {
                return;
            }

            picker.cells = [];
            picker.grid.innerHTML = '';
            picker.grid.style.gridTemplateColumns = 'repeat(' + maxColumns + ', 18px)';

            for (var rowIndex = 1; rowIndex <= maxRows; rowIndex += 1) {
                for (var columnIndex = 1; columnIndex <= maxColumns; columnIndex += 1) {
                    var cell = document.createElement('button');
                    cell.type = 'button';
                    cell.className = 'board-table-picker__cell';
                    cell.dataset.row = String(rowIndex);
                    cell.dataset.column = String(columnIndex);
                    cell.setAttribute('aria-label', rowIndex + '행 ' + columnIndex + '열');
                    picker.grid.appendChild(cell);
                    picker.cells.push(cell);
                }
            }
        }

        function updateTablePickerSelection(rows, columns) {
            var picker = ensureTablePicker();
            if (!picker) {
                return;
            }

            var safeRows = Number.isInteger(rows) ? rows : 0;
            var safeColumns = Number.isInteger(columns) ? columns : 0;

            if (safeRows > 0 && safeColumns > 0) {
                picker.summary.textContent = safeRows + ' x ' + safeColumns;
            } else {
                picker.summary.textContent = '표 크기 선택';
            }

            for (var i = 0; i < picker.cells.length; i += 1) {
                var cell = picker.cells[i];
                var cellRow = Number(cell.dataset.row);
                var cellColumn = Number(cell.dataset.column);
                var active = safeRows > 0 && safeColumns > 0 && cellRow <= safeRows && cellColumn <= safeColumns;
                cell.classList.toggle('is-active', active);
            }
        }

        function positionTablePicker() {
            var picker = ensureTablePicker();
            if (!picker || !picker.button) {
                return;
            }

            var buttonRect = picker.button.getBoundingClientRect();
            var viewportPadding = 8;
            var spacing = 6;
            var pickerWidth = picker.root.offsetWidth;
            var pickerHeight = picker.root.offsetHeight;

            var top = buttonRect.bottom + spacing;
            var left = buttonRect.left;

            if (left + pickerWidth > window.innerWidth - viewportPadding) {
                left = window.innerWidth - viewportPadding - pickerWidth;
            }
            if (left < viewportPadding) {
                left = viewportPadding;
            }

            if (top + pickerHeight > window.innerHeight - viewportPadding) {
                top = buttonRect.top - pickerHeight - spacing;
            }
            if (top < viewportPadding) {
                top = viewportPadding;
            }

            picker.root.style.top = Math.round(top) + 'px';
            picker.root.style.left = Math.round(left) + 'px';
        }

        function closeTablePicker() {
            var picker = tablePickerState;
            if (!picker || !picker.isOpen) {
                return;
            }

            picker.root.hidden = true;
            picker.root.setAttribute('aria-hidden', 'true');
            picker.isOpen = false;
            updateTablePickerSelection(0, 0);

            if (picker.button) {
                picker.button.classList.remove('ql-active');
                picker.button = null;
            }

            if (picker.outsideClickHandler) {
                document.removeEventListener('mousedown', picker.outsideClickHandler, true);
                picker.outsideClickHandler = null;
            }
            if (picker.keydownHandler) {
                document.removeEventListener('keydown', picker.keydownHandler, true);
                picker.keydownHandler = null;
            }
            if (picker.resizeHandler) {
                window.removeEventListener('resize', picker.resizeHandler);
                picker.resizeHandler = null;
            }
        }

        function openTablePicker(config, buttonElement) {
            var picker = ensureTablePicker();
            if (!picker || !buttonElement) {
                return false;
            }

            if (picker.isOpen && picker.button === buttonElement) {
                closeTablePicker();
                return true;
            }

            closeTablePicker();

            picker.minRows = config.minRows;
            picker.maxRows = config.maxRows;
            picker.minColumns = config.minColumns;
            picker.maxColumns = config.maxColumns;
            picker.button = buttonElement;
            picker.isOpen = true;

            renderTablePickerCells(picker.maxRows, picker.maxColumns);
            updateTablePickerSelection(0, 0);
            picker.root.hidden = false;
            picker.root.setAttribute('aria-hidden', 'false');
            picker.button.classList.add('ql-active');
            positionTablePicker();

            picker.outsideClickHandler = function (event) {
                if (picker.root.contains(event.target)) {
                    return;
                }
                if (picker.button && picker.button.contains(event.target)) {
                    return;
                }
                closeTablePicker();
            };
            picker.keydownHandler = function (event) {
                if (event.key === 'Escape') {
                    event.preventDefault();
                    closeTablePicker();
                    if (picker.button && typeof picker.button.focus === 'function') {
                        picker.button.focus();
                    }
                }
            };
            picker.resizeHandler = function () {
                if (picker.isOpen) {
                    positionTablePicker();
                }
            };

            document.addEventListener('mousedown', picker.outsideClickHandler, true);
            document.addEventListener('keydown', picker.keydownHandler, true);
            window.addEventListener('resize', picker.resizeHandler);
            return true;
        }

        function insertTableWithSize(rows, columns) {
            var tableModule = quill.getModule('table');
            if (tableModule && typeof tableModule.insertTable === 'function') {
                tableModule.insertTable(rows, columns);
                scheduleDraftSave();
                return;
            }

            var range = quill.getSelection(true);
            var index = range ? range.index : quill.getLength();
            quill.clipboard.dangerouslyPasteHTML(index, buildTableHtml(rows, columns), 'user');
            quill.setSelection(index + 1, 0, 'silent');
            scheduleDraftSave();
        }

        function buildTableHtml(rows, columns) {
            var html = '<table><tbody>';

            for (var rowIndex = 0; rowIndex < rows; rowIndex += 1) {
                html += '<tr>';
                for (var columnIndex = 0; columnIndex < columns; columnIndex += 1) {
                    html += '<td></td>';
                }
                html += '</tr>';
            }

            html += '</tbody></table><p><br></p>';
            return html;
        }

        function insertDefaultTable(buttonElement) {
            if (isCodeView) {
                window.alert('코드뷰에서는 테이블 버튼을 사용할 수 없습니다. 코드뷰를 종료해주세요.');
                return;
            }

            var minRows = normalizeTableSize(options.minTableRows, 1, 30, 1);
            var maxRows = normalizeTableSize(options.maxTableRows, minRows, 30, 10);
            var minColumns = normalizeTableSize(options.minTableColumns, 1, 30, 1);
            var maxColumns = normalizeTableSize(options.maxTableColumns, minColumns, 30, 10);
            var defaultRows = normalizeTableSize(options.defaultTableRows, minRows, maxRows, minRows);
            var defaultColumns = normalizeTableSize(options.defaultTableColumns, minColumns, maxColumns, minColumns);

            var opened = openTablePicker({
                minRows: minRows,
                maxRows: maxRows,
                minColumns: minColumns,
                maxColumns: maxColumns,
                defaultRows: defaultRows,
                defaultColumns: defaultColumns
            }, buttonElement);

            if (!opened) {
                insertTableWithSize(defaultRows, defaultColumns);
            }
        }

        function getDraftPayload() {
            var titleInput = document.querySelector(options.titleSelector);
            var sectionInput = document.querySelector(options.sectionSelector);
            var contents = readEditorHtml();
            return {
                title: titleInput ? titleInput.value : '',
                sectionId: sectionInput ? sectionInput.value : '',
                contents: contents,
                savedAt: new Date().toISOString()
            };
        }

        function hasMeaningfulDraft(payload) {
            if (!payload) {
                return false;
            }
            var text = extractTextFromHtml(payload.contents);
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

        function saveDraft(saveOptions) {
            if (!window.localStorage) {
                return;
            }

            var isManualSave = !!(saveOptions && saveOptions.manual);
            var payload = getDraftPayload();
            try {
                if (!hasMeaningfulDraft(payload)) {
                    window.localStorage.removeItem(options.draftStorageKey);
                    if (isManualSave) {
                        setDraftStatusMessage('저장할 내용이 없습니다.');
                    }
                    return;
                }
                window.localStorage.setItem(options.draftStorageKey, JSON.stringify(payload));
                if (isManualSave) {
                    var clock = formatSavedClock(payload.savedAt);
                    setDraftStatusMessage(clock ? ('임시저장 완료 (' + clock + ')') : '임시저장 완료');
                }
            } catch (ignored) {
                if (isManualSave) {
                    setDraftStatusMessage('임시저장에 실패했습니다.');
                }
            }
        }

        function scheduleDraftSave() {
            if (draftSaveTimer) {
                window.clearTimeout(draftSaveTimer);
            }
            draftSaveTimer = window.setTimeout(function () {
                syncHiddenInput();
                saveDraft();
            }, 400);
        }

        function clearDraftStorage() {
            if (!window.localStorage) {
                return;
            }
            try {
                window.localStorage.removeItem(options.draftStorageKey);
                setDraftStatusMessage('');
            } catch (ignored) {
            }
        }

        function bindDraftEvents() {
            var titleInput = document.querySelector(options.titleSelector);
            var sectionInput = document.querySelector(options.sectionSelector);
            var formElement = document.querySelector(options.formSelector);
            var manualSaveButton = document.querySelector(options.manualDraftSaveButtonSelector);

            if (titleInput) {
                titleInput.addEventListener('input', scheduleDraftSave);
            }
            if (sectionInput) {
                sectionInput.addEventListener('change', scheduleDraftSave);
            }
            codeViewTextarea.addEventListener('input', scheduleDraftSave);
            if (manualSaveButton) {
                manualSaveButton.addEventListener('click', function () {
                    syncHiddenInput();
                    saveDraft({ manual: true });
                });
            }
            if (formElement) {
                formElement.addEventListener('submit', function () {
                    syncHiddenInput();
                    clearDraftStorage();
                });
            }
        }

        function restoreDraft() {
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
                setEditorContents(draft.contents);
                syncHiddenInput();
            }
            if (draft.savedAt) {
                var restoredClock = formatSavedClock(draft.savedAt);
                setDraftStatusMessage(restoredClock ? ('임시저장 불러옴 (' + restoredClock + ')') : '임시저장 불러옴');
            }
        }

        function resolveImageUrl(responseText) {
            var raw = responseText;
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

        function resolveUploadErrorMessage(error) {
            if (!error) {
                return '이미지 업로드에 실패했습니다.';
            }
            if (error.status === 401) {
                return '로그인이 필요합니다. 다시 로그인해주세요.';
            }
            if (error.status === 403) {
                return '이미지 업로드 권한이 없습니다.';
            }
            if (error.status === 413) {
                return '이미지 용량이 너무 큽니다.';
            }
            if (error.status >= 500) {
                return '서버 오류로 이미지 업로드에 실패했습니다.';
            }

            var responseText = (error.responseText || '').trim();
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

        function insertImageAtCursor(imageUrl) {
            var range = quill.getSelection(true);
            var index = range ? range.index : quill.getLength();
            quill.insertEmbed(index, 'image', imageUrl, 'user');
            quill.setSelection(index + 1, 0, 'silent');
            scheduleDraftSave();
        }

        function createUploadError(status, responseText) {
            return {
                status: status,
                responseText: responseText
            };
        }

        function buildUploadHeaders() {
            var headers = {
                'X-Requested-With': 'XMLHttpRequest'
            };
            var csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
            var csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
            var csrfToken = csrfTokenMeta ? csrfTokenMeta.getAttribute('content') : '';
            var csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : '';

            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }
            return headers;
        }

        function uploadFile(file, retryCount) {
            if (!file || !file.type || !file.type.startsWith('image/')) {
                window.alert('이미지 파일만 업로드할 수 있습니다.');
                return Promise.resolve(false);
            }
            if (file.size > options.maxImageUploadSizeBytes) {
                window.alert('이미지는 10MB 이하만 업로드할 수 있습니다.');
                return Promise.resolve(false);
            }

            var attempt = typeof retryCount === 'number' ? retryCount : 0;
            var formData = new FormData();
            formData.append('file', file);

            currentUploadCount += 1;
            setEditorUploading(true);

            return window.fetch(options.uploadUrl, {
                method: 'POST',
                headers: buildUploadHeaders(),
                body: formData,
                credentials: 'same-origin'
            })
                .then(function (response) {
                    return response.text().then(function (responseText) {
                        if (!response.ok) {
                            throw createUploadError(response.status, responseText);
                        }
                        return responseText;
                    });
                })
                .then(function (responseText) {
                    var imageUrl = resolveImageUrl(responseText);
                    if (!imageUrl) {
                        window.alert('이미지 URL 처리에 실패했습니다.');
                        return false;
                    }
                    insertImageAtCursor(imageUrl);
                    return true;
                })
                .catch(function (error) {
                    var errorMessage = resolveUploadErrorMessage(error);
                    if (attempt < 1 && window.confirm(errorMessage + '\n다시 시도할까요?')) {
                        return uploadFile(file, attempt + 1);
                    }
                    window.alert(errorMessage);
                    return false;
                })
                .finally(function () {
                    currentUploadCount = Math.max(0, currentUploadCount - 1);
                    setEditorUploading(currentUploadCount > 0);
                });
        }

        function extractImageFiles(dataTransfer) {
            if (!dataTransfer) {
                return [];
            }

            var files = [];
            if (dataTransfer.files && dataTransfer.files.length) {
                for (var i = 0; i < dataTransfer.files.length; i += 1) {
                    var file = dataTransfer.files[i];
                    if (file && file.type && file.type.startsWith('image/')) {
                        files.push(file);
                    }
                }
            }

            if (files.length > 0) {
                return files;
            }

            if (dataTransfer.items && dataTransfer.items.length) {
                for (var j = 0; j < dataTransfer.items.length; j += 1) {
                    var item = dataTransfer.items[j];
                    if (item && item.kind === 'file' && item.type && item.type.startsWith('image/')) {
                        var converted = item.getAsFile();
                        if (converted) {
                            files.push(converted);
                        }
                    }
                }
            }
            return files;
        }

        function handleImageFiles(files) {
            if (!files || files.length === 0) {
                return;
            }
            if (currentUploadCount > 0) {
                window.alert('이미지 업로드가 진행 중입니다. 잠시 후 다시 시도해주세요.');
                return;
            }
            for (var index = 0; index < files.length; index += 1) {
                uploadFile(files[index]);
            }
        }

        function handlePaste(event) {
            var clipboard = event && event.clipboardData;
            if (!clipboard) {
                return;
            }

            var imageFiles = extractImageFiles(clipboard);
            if (imageFiles.length > 0) {
                event.preventDefault();
                handleImageFiles(imageFiles);
                return;
            }

            var text = clipboard.getData('text/plain');
            if (typeof text !== 'string') {
                return;
            }

            event.preventDefault();

            var range = quill.getSelection(true);
            var index = range ? range.index : quill.getLength();
            var length = range ? range.length : 0;
            if (length > 0) {
                quill.deleteText(index, length, 'user');
            }
            quill.insertText(index, text, 'user');
            quill.setSelection(index + text.length, 0, 'silent');
        }

        function handleDrop(event) {
            var dataTransfer = event && event.dataTransfer;
            if (!dataTransfer) {
                return;
            }

            var imageFiles = extractImageFiles(dataTransfer);
            if (imageFiles.length === 0) {
                return;
            }

            event.preventDefault();
            quill.focus();
            handleImageFiles(imageFiles);
        }

        function handleGlobalKeydown(event) {
            if (!event || event.key !== 'Escape') {
                return;
            }
            if (tablePickerState && tablePickerState.isOpen) {
                closeTablePicker();
                event.preventDefault();
                return;
            }
            if (isCodeView) {
                toggleCodeView(false);
                event.preventDefault();
                return;
            }
            if (isFullscreen) {
                toggleFullscreen(false);
                event.preventDefault();
            }
        }

        quill.on('text-change', function () {
            if (pendingPlaceholderCleanup) {
                pendingPlaceholderCleanup = false;
                cleanupSinglePlaceholderTable();
            }
            scheduleDraftSave();
        });
        quill.root.addEventListener('keydown', handleEditorKeydown);
        quill.root.addEventListener('paste', handlePaste);
        quill.root.addEventListener('drop', handleDrop);
        document.addEventListener('keydown', handleGlobalKeydown);

        bindDraftEvents();
        restoreDraft();
    }

    window.PubCookBoardEditor = {
        init: init
    };
})(window, document);
