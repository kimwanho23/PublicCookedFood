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
        maxTableColumns: 10,
        placeholder: '내용을 입력하세요.'
    };

    var tiptapRuntimePromise = null;

    function tiptapModuleUrl(packageName) {
        return 'https://esm.sh/' + packageName + '@2.22.3';
    }

    function loadTiptapRuntime() {
        if (tiptapRuntimePromise) {
            return tiptapRuntimePromise;
        }

        tiptapRuntimePromise = Promise.all([
            import(tiptapModuleUrl('@tiptap/core')),
            import(tiptapModuleUrl('@tiptap/starter-kit')),
            import(tiptapModuleUrl('@tiptap/extension-underline')),
            import(tiptapModuleUrl('@tiptap/extension-link')),
            import(tiptapModuleUrl('@tiptap/extension-image')),
            import(tiptapModuleUrl('@tiptap/extension-table')),
            import(tiptapModuleUrl('@tiptap/extension-table-row')),
            import(tiptapModuleUrl('@tiptap/extension-table-header')),
            import(tiptapModuleUrl('@tiptap/extension-table-cell')),
            import(tiptapModuleUrl('@tiptap/extension-placeholder')),
            import(tiptapModuleUrl('@tiptap/extension-youtube')),
            import(tiptapModuleUrl('@tiptap/extension-text-style'))
        ]).then(function (modules) {
            var textStyleModule = modules[11] || {};
            return {
                Editor: modules[0].Editor,
                Extension: modules[0].Extension,
                StarterKit: modules[1].default,
                Underline: modules[2].default,
                Link: modules[3].default,
                Image: modules[4].default,
                Table: modules[5].default,
                TableRow: modules[6].default,
                TableHeader: modules[7].default,
                TableCell: modules[8].default,
                Placeholder: modules[9].default,
                Youtube: modules[10].default,
                TextStyle: textStyleModule.default || textStyleModule.TextStyle || null
            };
        });

        return tiptapRuntimePromise;
    }

    function init(userOptions) {
        var options = Object.assign({}, DEFAULT_OPTIONS, userOptions || {});
        var editorContainer = document.querySelector(options.editorContainerSelector);
        var hiddenInput = document.querySelector(options.hiddenInputSelector);
        if (!editorContainer || !hiddenInput) {
            return;
        }

        if (editorContainer.dataset.boardEditorInitialized === 'true'
            || editorContainer.dataset.boardEditorInitialized === 'loading') {
            return;
        }
        editorContainer.dataset.boardEditorInitialized = 'loading';

        loadTiptapRuntime()
            .then(function (runtime) {
                setupEditor(runtime, options, editorContainer, hiddenInput);
                editorContainer.dataset.boardEditorInitialized = 'true';
            })
            .catch(function (error) {
                console.error('[BoardEditor] Failed to load Tiptap runtime.', error);
                editorContainer.dataset.boardEditorInitialized = 'false';
                window.alert('에디터 초기화에 실패했습니다. 새로고침 후 다시 시도해주세요.');
            });
    }

    function setupEditor(runtime, options, editorContainer, hiddenInput) {
        var currentUploadCount = 0;
        var imagePickerInput = null;
        var draftSaveTimer = null;
        var isCodeView = false;
        var isFullscreen = false;
        var tablePickerState = null;
        var tableMenuState = null;
        var draftStatusElement = null;
        var suppressEditorUpdate = false;
        var lastKnownSelectionRange = null;

        var toolbarContainer = buildToolbar();
        editorContainer.insertAdjacentElement('beforebegin', toolbarContainer);

        var editableElement = document.createElement('div');
        editableElement.className = 'board-tiptap-editor';
        editableElement.setAttribute('spellcheck', 'false');
        editableElement.setAttribute('autocomplete', 'off');
        editableElement.setAttribute('autocorrect', 'off');
        editableElement.setAttribute('autocapitalize', 'off');
        editorContainer.appendChild(editableElement);

        var codeViewTextarea = document.createElement('textarea');
        codeViewTextarea.className = 'board-quill-codeview';
        codeViewTextarea.setAttribute('spellcheck', 'false');
        codeViewTextarea.setAttribute('autocomplete', 'off');
        codeViewTextarea.setAttribute('autocorrect', 'off');
        codeViewTextarea.setAttribute('autocapitalize', 'off');
        editorContainer.appendChild(codeViewTextarea);

        if (editorContainer.classList.contains('board-quill-shell-invalid')) {
            toolbarContainer.classList.add('board-quill-toolbar-invalid');
        }

        function createBoardFontStyleExtension() {
            if (!runtime.Extension || !runtime.TextStyle) {
                return null;
            }

            return runtime.Extension.create({
                name: 'boardFontStyle',
                addGlobalAttributes: function () {
                    return [{
                        types: ['textStyle'],
                        attributes: {
                            fontFamily: {
                                default: null,
                                parseHTML: function (element) {
                                    var value = element && element.style ? element.style.fontFamily : '';
                                    return value && value.trim() ? value : null;
                                },
                                renderHTML: function (attributes) {
                                    if (!attributes.fontFamily) {
                                        return {};
                                    }
                                    return {
                                        style: 'font-family: ' + attributes.fontFamily
                                    };
                                }
                            },
                            fontSize: {
                                default: null,
                                parseHTML: function (element) {
                                    var value = element && element.style ? element.style.fontSize : '';
                                    return value && value.trim() ? value : null;
                                },
                                renderHTML: function (attributes) {
                                    if (!attributes.fontSize) {
                                        return {};
                                    }
                                    return {
                                        style: 'font-size: ' + attributes.fontSize
                                    };
                                }
                            }
                        }
                    }];
                },
                addCommands: function () {
                    return {
                        setFontFamily: function (fontFamily) {
                            return function (_ref) {
                                var chain = _ref.chain;
                                return chain().setMark('textStyle', { fontFamily: fontFamily }).run();
                            };
                        },
                        unsetFontFamily: function () {
                            return function (_ref) {
                                var chain = _ref.chain;
                                return chain().setMark('textStyle', { fontFamily: null }).removeEmptyTextStyle().run();
                            };
                        },
                        setFontSize: function (fontSize) {
                            return function (_ref) {
                                var chain = _ref.chain;
                                return chain().setMark('textStyle', { fontSize: fontSize }).run();
                            };
                        },
                        unsetFontSize: function () {
                            return function (_ref) {
                                var chain = _ref.chain;
                                return chain().setMark('textStyle', { fontSize: null }).removeEmptyTextStyle().run();
                            };
                        }
                    };
                }
            });
        }

        var extensions = [
            runtime.StarterKit.configure({
                heading: {
                    levels: [1, 2, 3]
                }
            }),
            runtime.Underline,
            runtime.Link.configure({
                openOnClick: false,
                autolink: false,
                linkOnPaste: true
            }),
            runtime.Image,
            runtime.Table.configure({
                resizable: true
            }),
            runtime.TableRow,
            runtime.TableHeader,
            runtime.TableCell,
            runtime.Placeholder.configure({
                placeholder: options.placeholder
            }),
            runtime.Youtube.configure({
                controls: true,
                nocookie: true,
                modestBranding: true
            })
        ];

        if (runtime.TextStyle) {
            extensions.push(runtime.TextStyle);
        }
        var boardFontStyleExtension = createBoardFontStyleExtension();
        if (boardFontStyleExtension) {
            extensions.push(boardFontStyleExtension);
        }

        var editor = new runtime.Editor({
            element: editableElement,
            extensions: extensions,
            content: normalizedEditorHtmlOrDefault(hiddenInput.value),
            onUpdate: function () {
                if (suppressEditorUpdate) {
                    return;
                }
                rememberCurrentSelectionRange();
                syncHiddenInput();
                scheduleDraftSave();
                updateToolbarToggleStates();
                syncTableSelectionClasses();
                refreshTableMenuVisibility();
            },
            onSelectionUpdate: function () {
                rememberCurrentSelectionRange();
                updateToolbarToggleStates();
                syncTableSelectionClasses();
                refreshTableMenuVisibility();
            }
        });

        rememberCurrentSelectionRange();
        syncHiddenInput();
        updateToolbarToggleStates();
        syncTableSelectionClasses();

        function createToolbarIconSvg(iconName) {
            var iconPaths = {
                bold: '<path d="M4 3.5h5.2a2.8 2.8 0 0 1 0 5.6H4z"></path><path d="M4 9.1h6a2.8 2.8 0 0 1 0 5.6H4z"></path>',
                italic: '<line x1="7" y1="3.5" x2="13" y2="3.5"></line><line x1="5" y1="14.5" x2="11" y2="14.5"></line><line x1="10" y1="3.5" x2="8" y2="14.5"></line>',
                underline: '<path d="M5 3.5v4.5a4 4 0 0 0 8 0V3.5"></path><line x1="4" y1="14.5" x2="14" y2="14.5"></line>',
                strike: '<line x1="3.5" y1="9" x2="14.5" y2="9"></line><path d="M12.8 5.7A3.7 3.7 0 0 0 9 4c-2.1 0-3.5 1-3.5 2.5 0 1.6 1.2 2.2 3.5 2.7 2.2.4 3.3.9 3.3 2.3 0 1.6-1.5 2.5-3.8 2.5A5.5 5.5 0 0 1 4.6 12"></path>',
                bulletList: '<circle cx="4.5" cy="5" r="1"></circle><circle cx="4.5" cy="9" r="1"></circle><circle cx="4.5" cy="13" r="1"></circle><line x1="7" y1="5" x2="14.5" y2="5"></line><line x1="7" y1="9" x2="14.5" y2="9"></line><line x1="7" y1="13" x2="14.5" y2="13"></line>',
                orderedList: '<path d="M3.8 4.4h1.7V6H3.8z"></path><path d="M3.8 8.1h1.7v1.6H3.8z"></path><path d="M3.8 11.8h1.9l-1.9 2.2h1.9"></path><line x1="7.5" y1="5" x2="14.5" y2="5"></line><line x1="7.5" y1="9" x2="14.5" y2="9"></line><line x1="7.5" y1="13" x2="14.5" y2="13"></line>',
                blockquote: '<path d="M5 6h3l-2 3h2v3H5z"></path><path d="M10 6h3l-2 3h2v3h-3z"></path>',
                codeBlock: '<polyline points="6.5 4.5 3.5 8.5 6.5 12.5"></polyline><polyline points="11.5 4.5 14.5 8.5 11.5 12.5"></polyline>',
                link: '<path d="M6.2 10.8H5a2.7 2.7 0 1 1 0-5.4h2.1"></path><path d="M9.8 5.4H11a2.7 2.7 0 1 1 0 5.4H8.9"></path><line x1="6.7" y1="8.1" x2="9.3" y2="8.1"></line>',
                image: '<rect x="3.2" y="4" width="11.6" height="9.6" rx="1.2"></rect><circle cx="6.5" cy="7.2" r="1"></circle><path d="M4.5 12l3.2-3 2.1 2 1.8-1.6 2.2 2.6"></path>',
                video: '<rect x="3.2" y="4" width="9.6" height="9.6" rx="1.2"></rect><polyline points="8.8 8.8 11.8 6.8 11.8 10.8 8.8 8.8"></polyline>',
                table: '<rect x="2.8" y="3.3" width="12.4" height="10.4" rx="0.8"></rect><line x1="2.8" y1="6.8" x2="15.2" y2="6.8"></line><line x1="2.8" y1="10.2" x2="15.2" y2="10.2"></line><line x1="7" y1="3.3" x2="7" y2="13.7"></line><line x1="11" y1="3.3" x2="11" y2="13.7"></line>',
                clean: '<line x1="4" y1="4" x2="14" y2="14"></line><line x1="14" y1="4" x2="4" y2="14"></line><line x1="3" y1="9" x2="15" y2="9"></line>',
                codeview: '<polyline points="6.5 5.5 3.5 8.5 6.5 11.5"></polyline><polyline points="11.5 5.5 14.5 8.5 11.5 11.5"></polyline>',
                fullscreen: '<polyline points="6.2 3.5 3.5 3.5 3.5 6.2"></polyline><polyline points="11.8 3.5 14.5 3.5 14.5 6.2"></polyline><polyline points="14.5 10.8 14.5 13.5 11.8 13.5"></polyline><polyline points="6.2 13.5 3.5 13.5 3.5 10.8"></polyline>'
            };
            return '<svg class="board-editor-icon" viewBox="0 0 18 18" aria-hidden="true" focusable="false">' + (iconPaths[iconName] || '') + '</svg>';
        }

        function createToolbarButtonHtml(action, title, iconName, extraClass) {
            var className = 'board-editor-btn';
            if (extraClass) {
                className += ' ' + extraClass;
            }
            return '<button type="button" class="' + className + '" data-action="' + action + '" title="' + title + '" aria-label="' + title + '">' + createToolbarIconSvg(iconName) + '</button>';
        }

        function createTableMenuIconSvg(iconName) {
            var iconPaths = {
                addRowBefore: '<line x1="3.5" y1="4.5" x2="14.5" y2="4.5"></line><line x1="3.5" y1="8.8" x2="14.5" y2="8.8"></line><line x1="3.5" y1="13.1" x2="14.5" y2="13.1"></line><line x1="9" y1="2.8" x2="9" y2="6.2"></line><line x1="7.3" y1="4.5" x2="10.7" y2="4.5"></line>',
                addRowAfter: '<line x1="3.5" y1="4.5" x2="14.5" y2="4.5"></line><line x1="3.5" y1="8.8" x2="14.5" y2="8.8"></line><line x1="3.5" y1="13.1" x2="14.5" y2="13.1"></line><line x1="9" y1="11.4" x2="9" y2="14.8"></line><line x1="7.3" y1="13.1" x2="10.7" y2="13.1"></line>',
                deleteRow: '<line x1="3.5" y1="4.5" x2="14.5" y2="4.5"></line><line x1="3.5" y1="8.8" x2="14.5" y2="8.8"></line><line x1="3.5" y1="13.1" x2="14.5" y2="13.1"></line><line x1="6.8" y1="8.8" x2="11.2" y2="8.8"></line>',
                addColumnBefore: '<line x1="4.5" y1="3.5" x2="4.5" y2="14.5"></line><line x1="8.8" y1="3.5" x2="8.8" y2="14.5"></line><line x1="13.1" y1="3.5" x2="13.1" y2="14.5"></line><line x1="2.8" y1="9" x2="6.2" y2="9"></line><line x1="4.5" y1="7.3" x2="4.5" y2="10.7"></line>',
                addColumnAfter: '<line x1="4.5" y1="3.5" x2="4.5" y2="14.5"></line><line x1="8.8" y1="3.5" x2="8.8" y2="14.5"></line><line x1="13.1" y1="3.5" x2="13.1" y2="14.5"></line><line x1="11.4" y1="9" x2="14.8" y2="9"></line><line x1="13.1" y1="7.3" x2="13.1" y2="10.7"></line>',
                deleteColumn: '<line x1="4.5" y1="3.5" x2="4.5" y2="14.5"></line><line x1="8.8" y1="3.5" x2="8.8" y2="14.5"></line><line x1="13.1" y1="3.5" x2="13.1" y2="14.5"></line><line x1="8.8" y1="6.8" x2="8.8" y2="11.2"></line><line x1="6.8" y1="9" x2="10.8" y2="9"></line>',
                mergeCells: '<rect x="3.5" y="3.5" width="11" height="11"></rect><line x1="9" y1="3.5" x2="9" y2="14.5"></line><polyline points="6.2 9 8.2 9 7.2 8"></polyline><polyline points="11.8 9 9.8 9 10.8 10"></polyline>',
                splitHorizontal: '<rect x="3.5" y="3.5" width="11" height="11"></rect><line x1="3.5" y1="9" x2="14.5" y2="9"></line><line x1="9" y1="7" x2="9" y2="11"></line>',
                splitVertical: '<rect x="3.5" y="3.5" width="11" height="11"></rect><line x1="9" y1="3.5" x2="9" y2="14.5"></line><line x1="7" y1="9" x2="11" y2="9"></line>',
                toggleHeaderRow: '<rect x="3.5" y="3.5" width="11" height="11"></rect><line x1="3.5" y1="7.3" x2="14.5" y2="7.3"></line>',
                toggleHeaderColumn: '<rect x="3.5" y="3.5" width="11" height="11"></rect><line x1="7.3" y1="3.5" x2="7.3" y2="14.5"></line>',
                deleteTable: '<rect x="3.5" y="3.5" width="11" height="11"></rect><line x1="5" y1="5" x2="13" y2="13"></line><line x1="13" y1="5" x2="5" y2="13"></line>'
            };
            return '<svg class="board-table-menu__icon" viewBox="0 0 18 18" aria-hidden="true" focusable="false">' + (iconPaths[iconName] || '') + '</svg>';
        }

        function createTableMenuButtonHtml(action, title, iconName, extraClass) {
            var className = 'board-table-menu__button';
            if (extraClass) {
                className += ' ' + extraClass;
            }
            return '<button type="button" class="' + className + '" data-table-action="' + action + '" title="' + title + '" aria-label="' + title + '">' + createTableMenuIconSvg(iconName) + '</button>';
        }

        function buildToolbar() {
            var toolbar = document.createElement('div');
            toolbar.className = 'board-quill-toolbar ql-toolbar ql-snow';

            var headingSelectHtml = [
                '<span class="ql-formats">',
                '  <select class="board-editor-font-family" title="글꼴">',
                '    <option value="default">기본 글꼴</option>',
                '    <option value="\'Noto Sans KR\', \'Noto Sans\', sans-serif">고딕</option>',
                '    <option value="\'Noto Serif KR\', serif">명조</option>',
                '    <option value="\'Malgun Gothic\', \'맑은 고딕\', sans-serif">맑은 고딕</option>',
                '    <option value="\'Nanum Gothic\', sans-serif">나눔고딕</option>',
                '    <option value="\'Nanum Myeongjo\', serif">나눔명조</option>',
                '    <option value="\'Gowun Batang\', serif">고운바탕</option>',
                '    <option value="\'Gothic A1\', sans-serif">고딕 A1</option>',
                '    <option value="\'IBM Plex Sans KR\', sans-serif">IBM Plex Sans KR</option>',
                '    <option value="\'Arial\', sans-serif">Arial</option>',
                '    <option value="\'Georgia\', serif">Georgia</option>',
                '    <option value="\'Courier New\', monospace">Courier New</option>',
                '    <option value="monospace">Monospace</option>',
                '    <option value="\'Gungsuh\', \'궁서\', serif">궁서</option>',
                '  </select>',
                '</span>',
                '<span class="ql-formats">',
                '  <select class="board-editor-font-size" title="글자 크기">',
                '    <option value="default">크기</option>',
                '    <option value="12px">12</option>',
                '    <option value="14px">14</option>',
                '    <option value="16px">16</option>',
                '    <option value="18px">18</option>',
                '    <option value="20px">20</option>',
                '    <option value="24px">24</option>',
                '    <option value="28px">28</option>',
                '    <option value="32px">32</option>',
                '  </select>',
                '</span>'
            ].join('');

            var markButtonsHtml = [
                '<span class="ql-formats">',
                createToolbarButtonHtml('bold', '굵게', 'bold'),
                createToolbarButtonHtml('italic', '기울임', 'italic'),
                createToolbarButtonHtml('underline', '밑줄', 'underline'),
                createToolbarButtonHtml('strike', '취소선', 'strike'),
                '</span>'
            ].join('');

            var listButtonsHtml = [
                '<span class="ql-formats">',
                createToolbarButtonHtml('bulletList', '점 목록', 'bulletList'),
                createToolbarButtonHtml('orderedList', '번호 목록', 'orderedList'),
                '</span>'
            ].join('');

            var richButtonsHtml = [
                '<span class="ql-formats">',
                createToolbarButtonHtml('blockquote', '인용', 'blockquote'),
                createToolbarButtonHtml('codeBlock', '코드', 'codeBlock'),
                createToolbarButtonHtml('link', '링크', 'link'),
                createToolbarButtonHtml('image', '이미지', 'image'),
                createToolbarButtonHtml('video', '동영상', 'video'),
                createToolbarButtonHtml('table', '표', 'table'),
                '</span>'
            ].join('');

            var miscButtonsHtml = [
                '<span class="ql-formats">',
                createToolbarButtonHtml('clean', '서식 제거', 'clean', 'board-editor-action-clean'),
                createToolbarButtonHtml('codeview', 'HTML 보기', 'codeview', 'board-editor-action-codeview'),
                createToolbarButtonHtml('fullscreen', '전체화면', 'fullscreen', 'board-editor-action-fullscreen'),
                '</span>'
            ].join('');

            toolbar.innerHTML = headingSelectHtml + markButtonsHtml + listButtonsHtml + richButtonsHtml + miscButtonsHtml;
            return toolbar;
        }

        function normalizedEditorHtmlOrDefault(html) {
            var normalized = normalizeEditorHtml(html);
            if (!normalized) {
                return '<p></p>';
            }
            return normalized;
        }

        function normalizeEditorHtml(html) {
            if (typeof html !== 'string') {
                return '';
            }
            var normalized = html.trim();
            if (!normalized || normalized === '<p><br></p>' || normalized === '<p></p>') {
                return '';
            }

            var normalizedRoot = document.createElement('div');
            normalizedRoot.innerHTML = normalized;
            if (isSinglePlaceholderTableDocument(normalizedRoot)) {
                return '';
            }
            return normalized;
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

        function readEditorHtml() {
            if (isCodeView) {
                return normalizeEditorHtml(codeViewTextarea.value);
            }
            return normalizeEditorHtml(editor.getHTML());
        }

        function syncHiddenInput() {
            var normalizedHtml = readEditorHtml();
            hiddenInput.value = normalizedHtml;

            if (!isCodeView && normalizedHtml === '' && editor.getHTML().indexOf('<table') !== -1) {
                clearEntireEditor();
            }
        }

        function setEditorContents(html) {
            var normalized = normalizeEditorHtml(html);
            var nextHtml = normalized || '<p></p>';
            suppressEditorUpdate = true;
            editor.commands.setContent(nextHtml, false);
            suppressEditorUpdate = false;
            codeViewTextarea.value = normalized;
        }

        function getEditorContentLength() {
            return (editor.state && editor.state.doc && editor.state.doc.content)
                ? editor.state.doc.content.size
                : 0;
        }

        function rememberCurrentSelectionRange() {
            if (!editor || !editor.state || !editor.state.selection) {
                return;
            }
            var selection = editor.state.selection;
            if (typeof selection.from !== 'number' || typeof selection.to !== 'number') {
                return;
            }
            lastKnownSelectionRange = {
                from: selection.from,
                to: selection.to
            };
        }

        function buildSelectionAwareChain() {
            var chain = editor.chain().focus();
            if (!lastKnownSelectionRange
                || typeof lastKnownSelectionRange.from !== 'number'
                || typeof lastKnownSelectionRange.to !== 'number') {
                return chain;
            }

            var maxPosition = Math.max(1, getEditorContentLength());
            var from = Math.max(1, Math.min(lastKnownSelectionRange.from, maxPosition));
            var to = Math.max(from, Math.min(lastKnownSelectionRange.to, maxPosition));
            try {
                chain = chain.setTextSelection({ from: from, to: to });
            } catch (ignored) {
            }
            return chain;
        }

        function isWholeDocumentSelection() {
            if (!editor.state || !editor.state.selection) {
                return false;
            }
            var selection = editor.state.selection;
            if (selection.empty) {
                return false;
            }
            var contentSize = getEditorContentLength();
            if (contentSize <= 0) {
                return false;
            }
            return selection.from <= 1 && selection.to >= contentSize;
        }

        function updateToolbarToggleStates() {
            var textStyleAttributes = editor.getAttributes('textStyle') || {};
            var currentFontFamily = normalizeFontFamilyValue(textStyleAttributes.fontFamily);
            var currentFontSize = normalizeFontSizeValue(textStyleAttributes.fontSize);

            var fontFamilySelect = toolbarContainer.querySelector('.board-editor-font-family');
            if (fontFamilySelect) {
                fontFamilySelect.value = 'default';
                for (var familyIndex = 0; familyIndex < fontFamilySelect.options.length; familyIndex += 1) {
                    var familyOption = fontFamilySelect.options[familyIndex];
                    if (normalizeFontFamilyValue(familyOption.value) === currentFontFamily && currentFontFamily) {
                        fontFamilySelect.value = familyOption.value;
                        break;
                    }
                }
            }

            var fontSizeSelect = toolbarContainer.querySelector('.board-editor-font-size');
            if (fontSizeSelect) {
                fontSizeSelect.value = 'default';
                for (var sizeIndex = 0; sizeIndex < fontSizeSelect.options.length; sizeIndex += 1) {
                    var sizeOption = fontSizeSelect.options[sizeIndex];
                    if (normalizeFontSizeValue(sizeOption.value) === currentFontSize && currentFontSize) {
                        fontSizeSelect.value = sizeOption.value;
                        break;
                    }
                }
            }

            setButtonActive('bold', editor.isActive('bold'));
            setButtonActive('italic', editor.isActive('italic'));
            setButtonActive('underline', editor.isActive('underline'));
            setButtonActive('strike', editor.isActive('strike'));
            setButtonActive('bulletList', editor.isActive('bulletList'));
            setButtonActive('orderedList', editor.isActive('orderedList'));
            setButtonActive('blockquote', editor.isActive('blockquote'));
            setButtonActive('codeBlock', editor.isActive('codeBlock'));
            setButtonActive('link', editor.isActive('link'));
            setButtonActive('codeview', isCodeView);
            setButtonActive('fullscreen', isFullscreen);
        }

        function normalizeFontFamilyValue(value) {
            if (typeof value !== 'string') {
                return '';
            }
            return value.replace(/["']/g, '').replace(/\s+/g, ' ').trim().toLowerCase();
        }

        function normalizeFontSizeValue(value) {
            if (typeof value !== 'string') {
                return '';
            }
            return value.trim().toLowerCase();
        }

        function setButtonActive(action, active) {
            var button = toolbarContainer.querySelector('[data-action="' + action + '"]');
            if (!button) {
                return;
            }
            button.classList.toggle('ql-active', !!active);
        }

        function getActiveTableCellElement() {
            var selectedCell = editableElement.querySelector('td.selectedCell, th.selectedCell');
            if (selectedCell) {
                return selectedCell;
            }

            var nativeSelection = window.getSelection();
            if (!nativeSelection || nativeSelection.rangeCount <= 0) {
                return null;
            }

            var anchor = nativeSelection.anchorNode;
            var anchorElement = anchor && anchor.nodeType === 1 ? anchor : (anchor ? anchor.parentElement : null);
            if (!anchorElement || typeof anchorElement.closest !== 'function') {
                return null;
            }
            var cell = anchorElement.closest('td,th');
            if (!cell || !editableElement.contains(cell)) {
                return null;
            }
            return cell;
        }

        function syncTableSelectionClasses() {
            var previousSelectedTables = editableElement.querySelectorAll('table.is-selected-table');
            for (var i = 0; i < previousSelectedTables.length; i += 1) {
                previousSelectedTables[i].classList.remove('is-selected-table');
            }
            var previousActiveCells = editableElement.querySelectorAll('td.is-active-table-cell, th.is-active-table-cell');
            for (var activeIndex = 0; activeIndex < previousActiveCells.length; activeIndex += 1) {
                previousActiveCells[activeIndex].classList.remove('is-active-table-cell');
            }

            var selectedCells = editableElement.querySelectorAll('td.selectedCell, th.selectedCell');
            if (selectedCells.length > 0) {
                for (var idx = 0; idx < selectedCells.length; idx += 1) {
                    var tableFromSelected = selectedCells[idx].closest('table');
                    if (tableFromSelected) {
                        tableFromSelected.classList.add('is-selected-table');
                    }
                }
                return;
            }

            var activeCell = getActiveTableCellElement();
            if (!activeCell) {
                return;
            }
            var activeTable = activeCell.closest('table');
            if (activeTable) {
                activeTable.classList.add('is-selected-table');
                activeCell.classList.add('is-active-table-cell');
            }
        }

        function canRunTableCommand(action) {
            try {
                var chain = editor.can().chain().focus();
                switch (action) {
                    case 'addRowBefore':
                        return chain.addRowBefore().run();
                    case 'addRowAfter':
                        return chain.addRowAfter().run();
                    case 'deleteRow':
                        return chain.deleteRow().run();
                    case 'addColumnBefore':
                        return chain.addColumnBefore().run();
                    case 'addColumnAfter':
                        return chain.addColumnAfter().run();
                    case 'deleteColumn':
                        return chain.deleteColumn().run();
                    case 'mergeCells':
                        return chain.mergeCells().run();
                    case 'splitHorizontal':
                        return editor.can().chain().focus().splitCell().run()
                            || editor.can().chain().focus().addRowAfter().run();
                    case 'splitVertical':
                        return editor.can().chain().focus().splitCell().run()
                            || editor.can().chain().focus().addColumnAfter().run();
                    case 'toggleHeaderRow':
                        return chain.toggleHeaderRow().run();
                    case 'toggleHeaderColumn':
                        return chain.toggleHeaderColumn().run();
                    case 'deleteTable':
                        return chain.deleteTable().run();
                    default:
                        return false;
                }
            } catch (ignored) {
                return false;
            }
        }

        function runTableMenuAction(action) {
            var ran = false;
            switch (action) {
                case 'addRowBefore':
                    ran = editor.chain().focus().addRowBefore().run();
                    break;
                case 'addRowAfter':
                    ran = editor.chain().focus().addRowAfter().run();
                    break;
                case 'deleteRow':
                    ran = editor.chain().focus().deleteRow().run();
                    break;
                case 'addColumnBefore':
                    ran = editor.chain().focus().addColumnBefore().run();
                    break;
                case 'addColumnAfter':
                    ran = editor.chain().focus().addColumnAfter().run();
                    break;
                case 'deleteColumn':
                    ran = editor.chain().focus().deleteColumn().run();
                    break;
                case 'mergeCells':
                    ran = editor.chain().focus().mergeCells().run();
                    break;
                case 'splitHorizontal':
                    ran = editor.chain().focus().splitCell().run();
                    if (!ran) {
                        ran = editor.chain().focus().addRowAfter().run();
                    }
                    break;
                case 'splitVertical':
                    ran = editor.chain().focus().splitCell().run();
                    if (!ran) {
                        ran = editor.chain().focus().addColumnAfter().run();
                    }
                    break;
                case 'toggleHeaderRow':
                    ran = editor.chain().focus().toggleHeaderRow().run();
                    break;
                case 'toggleHeaderColumn':
                    ran = editor.chain().focus().toggleHeaderColumn().run();
                    break;
                case 'deleteTable':
                    ran = editor.chain().focus().deleteTable().run();
                    break;
                default:
                    ran = false;
                    break;
            }

            if (!ran) {
                return;
            }

            syncHiddenInput();
            scheduleDraftSave();
            syncTableSelectionClasses();
            refreshTableMenuVisibility();
        }

        function ensureTableMenu() {
            if (tableMenuState) {
                return tableMenuState;
            }

            var menuRoot = document.createElement('div');
            menuRoot.className = 'board-table-menu';
            menuRoot.hidden = true;
            menuRoot.setAttribute('aria-hidden', 'true');

            menuRoot.innerHTML = [
                '<div class="board-table-menu__row">',
                createTableMenuButtonHtml('addRowBefore', '위 행 추가', 'addRowBefore'),
                createTableMenuButtonHtml('addRowAfter', '아래 행 추가', 'addRowAfter'),
                createTableMenuButtonHtml('deleteRow', '행 삭제', 'deleteRow'),
                '</div>',
                '<div class="board-table-menu__row">',
                createTableMenuButtonHtml('addColumnBefore', '왼쪽 열 추가', 'addColumnBefore'),
                createTableMenuButtonHtml('addColumnAfter', '오른쪽 열 추가', 'addColumnAfter'),
                createTableMenuButtonHtml('deleteColumn', '열 삭제', 'deleteColumn'),
                '</div>',
                '<div class="board-table-menu__row">',
                createTableMenuButtonHtml('mergeCells', '셀 병합', 'mergeCells'),
                createTableMenuButtonHtml('splitHorizontal', '수평 분할', 'splitHorizontal'),
                createTableMenuButtonHtml('splitVertical', '수직 분할', 'splitVertical'),
                createTableMenuButtonHtml('toggleHeaderRow', '헤더 행 토글', 'toggleHeaderRow'),
                createTableMenuButtonHtml('toggleHeaderColumn', '헤더 열 토글', 'toggleHeaderColumn'),
                '</div>',
                '<div class="board-table-menu__row">',
                createTableMenuButtonHtml('deleteTable', '표 삭제', 'deleteTable', 'is-danger'),
                '</div>'
            ].join('');

            document.body.appendChild(menuRoot);

            tableMenuState = {
                root: menuRoot,
                anchorCell: null,
                isOpen: false,
                outsideClickHandler: null,
                keydownHandler: null,
                resizeHandler: null,
                scrollHandler: null
            };

            menuRoot.addEventListener('click', function (event) {
                var button = event.target.closest('[data-table-action]');
                if (!button || !menuRoot.contains(button)) {
                    return;
                }
                event.preventDefault();

                var action = button.getAttribute('data-table-action');
                if (!action) {
                    return;
                }
                runTableMenuAction(action);
            });

            return tableMenuState;
        }

        function updateTableMenuButtonStates() {
            var menu = ensureTableMenu();
            if (!menu) {
                return;
            }

            var actionButtons = menu.root.querySelectorAll('[data-table-action]');
            for (var i = 0; i < actionButtons.length; i += 1) {
                var button = actionButtons[i];
                var action = button.getAttribute('data-table-action');
                button.disabled = !canRunTableCommand(action);
            }
        }

        function positionTableMenu() {
            var menu = tableMenuState;
            if (!menu || !menu.isOpen || !menu.anchorCell) {
                return;
            }

            var cellRect = menu.anchorCell.getBoundingClientRect();
            var viewportPadding = 8;
            var spacing = 6;
            var menuWidth = menu.root.offsetWidth;
            var menuHeight = menu.root.offsetHeight;

            var top = cellRect.bottom + spacing;
            var left = cellRect.left;

            if (left + menuWidth > window.innerWidth - viewportPadding) {
                left = window.innerWidth - viewportPadding - menuWidth;
            }
            if (left < viewportPadding) {
                left = viewportPadding;
            }
            if (top + menuHeight > window.innerHeight - viewportPadding) {
                top = cellRect.top - menuHeight - spacing;
            }
            if (top < viewportPadding) {
                top = viewportPadding;
            }

            menu.root.style.top = Math.round(top) + 'px';
            menu.root.style.left = Math.round(left) + 'px';
        }

        function closeTableMenu() {
            var menu = tableMenuState;
            if (!menu || !menu.isOpen) {
                return;
            }

            menu.root.hidden = true;
            menu.root.setAttribute('aria-hidden', 'true');
            menu.isOpen = false;
            menu.anchorCell = null;

            if (menu.outsideClickHandler) {
                document.removeEventListener('mousedown', menu.outsideClickHandler, true);
                menu.outsideClickHandler = null;
            }
            if (menu.keydownHandler) {
                document.removeEventListener('keydown', menu.keydownHandler, true);
                menu.keydownHandler = null;
            }
            if (menu.resizeHandler) {
                window.removeEventListener('resize', menu.resizeHandler);
                menu.resizeHandler = null;
            }
            if (menu.scrollHandler) {
                window.removeEventListener('scroll', menu.scrollHandler, true);
                menu.scrollHandler = null;
            }
        }

        function openTableMenu(anchorCell) {
            var menu = ensureTableMenu();
            if (!menu || !anchorCell) {
                return;
            }

            if (menu.isOpen && menu.anchorCell === anchorCell) {
                updateTableMenuButtonStates();
                positionTableMenu();
                return;
            }

            closeTableMenu();

            menu.anchorCell = anchorCell;
            menu.isOpen = true;
            menu.root.hidden = false;
            menu.root.setAttribute('aria-hidden', 'false');
            updateTableMenuButtonStates();
            positionTableMenu();

            menu.outsideClickHandler = function (event) {
                if (menu.root.contains(event.target)) {
                    return;
                }
                if (editableElement.contains(event.target)) {
                    window.requestAnimationFrame(refreshTableMenuVisibility);
                    return;
                }
                closeTableMenu();
            };
            menu.keydownHandler = function (event) {
                if (event.key === 'Escape') {
                    event.preventDefault();
                    closeTableMenu();
                }
            };
            menu.resizeHandler = function () {
                positionTableMenu();
            };
            menu.scrollHandler = function () {
                positionTableMenu();
            };

            document.addEventListener('mousedown', menu.outsideClickHandler, true);
            document.addEventListener('keydown', menu.keydownHandler, true);
            window.addEventListener('resize', menu.resizeHandler);
            window.addEventListener('scroll', menu.scrollHandler, true);
        }

        function refreshTableMenuVisibility() {
            if (isCodeView || (tablePickerState && tablePickerState.isOpen)) {
                closeTableMenu();
                return;
            }

            var activeCell = getActiveTableCellElement();
            if (!activeCell || !editor.isActive('table')) {
                closeTableMenu();
                return;
            }

            openTableMenu(activeCell);
        }

        function setEditorUploading(uploading) {
            editorContainer.classList.toggle('is-uploading', !!uploading);
            toolbarContainer.classList.toggle('is-uploading', !!uploading);
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
            closeTableMenu();

            isFullscreen = nextState;
            editorContainer.classList.toggle('is-fullscreen', isFullscreen);
            toolbarContainer.classList.toggle('is-fullscreen', isFullscreen);
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
            closeTableMenu();

            if (nextState) {
                syncHiddenInput();
                codeViewTextarea.value = hiddenInput.value;
                editorContainer.classList.add('is-codeview');
                toolbarContainer.classList.add('is-codeview');
                editor.setEditable(false);
                codeViewTextarea.focus();
            } else {
                setEditorContents(codeViewTextarea.value);
                editorContainer.classList.remove('is-codeview');
                toolbarContainer.classList.remove('is-codeview');
                editor.setEditable(true);
                editor.commands.focus('end');
            }

            isCodeView = nextState;
            syncHiddenInput();
            scheduleDraftSave();
            updateToolbarToggleStates();
        }

        function clearEntireEditor() {
            closeTablePicker();
            closeTableMenu();
            setEditorContents('');
            editor.commands.focus('start');
            syncHiddenInput();
            scheduleDraftSave();
        }

        function normalizeYoutubeUrl(rawUrl) {
            if (typeof rawUrl !== 'string') {
                return null;
            }

            var candidate = rawUrl.trim();
            if (!candidate) {
                return null;
            }

            try {
                return new URL(candidate, window.location.origin).toString();
            } catch (ignored) {
                return null;
            }
        }

        function openVideoPrompt() {
            if (isCodeView) {
                window.alert('코드뷰에서는 동영상 버튼을 사용할 수 없습니다. 코드뷰를 종료해주세요.');
                return;
            }

            var input = window.prompt('YouTube URL을 입력하세요.');
            if (input == null) {
                return;
            }

            var normalizedVideoUrl = normalizeYoutubeUrl(input);
            if (!normalizedVideoUrl) {
                window.alert('올바른 URL을 입력해주세요.');
                return;
            }

            editor.chain().focus().setYoutubeVideo({
                src: normalizedVideoUrl,
                width: 640,
                height: 360
            }).run();
        }

        function handleClearFormatting() {
            closeTablePicker();
            closeTableMenu();

            if (isWholeDocumentSelection()) {
                clearEntireEditor();
                return;
            }

            if (editor.isActive('table')) {
                if (editor.chain().focus().deleteTable().run()) {
                    syncHiddenInput();
                    scheduleDraftSave();
                    return;
                }
            }

            editor.chain().focus().unsetAllMarks().clearNodes().run();
            syncHiddenInput();
            scheduleDraftSave();
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
            closeTableMenu();

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
            editor.chain().focus().insertTable({
                rows: rows,
                cols: columns,
                withHeaderRow: false
            }).run();
            scheduleDraftSave();
        }

        function insertDefaultTable(buttonElement) {
            if (isCodeView) {
                window.alert('코드뷰에서는 테이블 버튼을 사용할 수 없습니다. 코드뷰를 종료해주세요.');
                return;
            }
            closeTableMenu();

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

        function extractTextFromHtml(html) {
            if (!html) {
                return '';
            }
            var temp = document.createElement('div');
            temp.innerHTML = html;
            return (temp.textContent || '').replace(/\s+/g, '');
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
            editor.chain().focus().setImage({ src: imageUrl }).run();
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
            }
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
            editor.commands.focus();
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
            if (tableMenuState && tableMenuState.isOpen) {
                closeTableMenu();
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

        function handleToolbarClick(event) {
            var button = event.target.closest('[data-action]');
            if (!button || !toolbarContainer.contains(button)) {
                return;
            }
            event.preventDefault();

            var action = button.dataset.action;
            if (!action) {
                return;
            }

            if (action !== 'codeview' && isCodeView) {
                window.alert('코드뷰를 종료한 뒤 사용해주세요.');
                return;
            }

            switch (action) {
                case 'bold':
                    editor.chain().focus().toggleBold().run();
                    break;
                case 'italic':
                    editor.chain().focus().toggleItalic().run();
                    break;
                case 'underline':
                    editor.chain().focus().toggleUnderline().run();
                    break;
                case 'strike':
                    editor.chain().focus().toggleStrike().run();
                    break;
                case 'bulletList':
                    editor.chain().focus().toggleBulletList().run();
                    break;
                case 'orderedList':
                    editor.chain().focus().toggleOrderedList().run();
                    break;
                case 'blockquote':
                    editor.chain().focus().toggleBlockquote().run();
                    break;
                case 'codeBlock':
                    editor.chain().focus().toggleCodeBlock().run();
                    break;
                case 'link':
                    openLinkPrompt();
                    break;
                case 'image':
                    openImagePicker();
                    break;
                case 'video':
                    openVideoPrompt();
                    break;
                case 'table':
                    insertDefaultTable(button);
                    break;
                case 'clean':
                    handleClearFormatting();
                    break;
                case 'codeview':
                    toggleCodeView();
                    break;
                case 'fullscreen':
                    toggleFullscreen();
                    break;
                default:
                    break;
            }

            updateToolbarToggleStates();
            syncTableSelectionClasses();
            refreshTableMenuVisibility();
        }

        function handleToolbarSelectChange(event) {
            var select = event.target.closest('.board-editor-font-family, .board-editor-font-size');
            if (!select || !toolbarContainer.contains(select)) {
                return;
            }

            if (isCodeView) {
                return;
            }

            var chain = buildSelectionAwareChain();

            if (select.classList.contains('board-editor-font-family')) {
                if (select.value === 'default') {
                    if (typeof editor.commands.unsetFontFamily === 'function') {
                        chain.unsetFontFamily().run();
                    } else {
                        chain.setMark('textStyle', { fontFamily: null }).run();
                    }
                } else if (typeof editor.commands.setFontFamily === 'function') {
                    chain.setFontFamily(select.value).run();
                } else {
                    chain.setMark('textStyle', { fontFamily: select.value }).run();
                }
            } else if (select.classList.contains('board-editor-font-size')) {
                if (select.value === 'default') {
                    if (typeof editor.commands.unsetFontSize === 'function') {
                        chain.unsetFontSize().run();
                    } else {
                        chain.setMark('textStyle', { fontSize: null }).run();
                    }
                } else if (typeof editor.commands.setFontSize === 'function') {
                    chain.setFontSize(select.value).run();
                } else {
                    chain.setMark('textStyle', { fontSize: select.value }).run();
                }
            }

            rememberCurrentSelectionRange();
            updateToolbarToggleStates();
            syncTableSelectionClasses();
            refreshTableMenuVisibility();
        }

        function openLinkPrompt() {
            var previousUrl = '';
            if (editor.isActive('link')) {
                previousUrl = editor.getAttributes('link').href || '';
            }

            var input = window.prompt('링크 URL을 입력하세요.', previousUrl);
            if (input == null) {
                return;
            }

            var trimmed = input.trim();
            if (!trimmed) {
                editor.chain().focus().unsetLink().run();
                return;
            }

            var normalized;
            try {
                normalized = new URL(trimmed, window.location.origin).toString();
            } catch (ignored) {
                window.alert('올바른 URL을 입력해주세요.');
                return;
            }

            editor.chain().focus().extendMarkRange('link').setLink({ href: normalized }).run();
        }

        toolbarContainer.addEventListener('mousedown', function (event) {
            if (event.target.closest('.board-editor-font-family, .board-editor-font-size')) {
                rememberCurrentSelectionRange();
            }
        });
        toolbarContainer.addEventListener('click', handleToolbarClick);
        toolbarContainer.addEventListener('change', handleToolbarSelectChange);

        editableElement.addEventListener('mouseup', function () {
            window.requestAnimationFrame(refreshTableMenuVisibility);
        });
        editableElement.addEventListener('keyup', function () {
            window.requestAnimationFrame(refreshTableMenuVisibility);
        });
        editableElement.addEventListener('paste', handlePaste);
        editableElement.addEventListener('drop', handleDrop);
        document.addEventListener('keydown', handleGlobalKeydown);

        bindDraftEvents();
        restoreDraft();

        syncHiddenInput();
    }

    window.PubCookBoardEditor = {
        init: init
    };
})(window, document);
