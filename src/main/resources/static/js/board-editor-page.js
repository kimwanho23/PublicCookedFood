(function (window, document) {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        if (!window.PubCookBoardEditor || typeof window.PubCookBoardEditor.init !== 'function') {
            return;
        }

        var formElement = document.querySelector('form[data-board-draft-storage-key]');
        if (!formElement) {
            return;
        }

        var draftStorageKey = formElement.getAttribute('data-board-draft-storage-key') || 'boardDraft:new';
        var restoreConfirmMessage = formElement.getAttribute('data-board-restore-confirm-message') || '';

        window.PubCookBoardEditor.init({
            draftStorageKey: draftStorageKey,
            restoreConfirmMessage: restoreConfirmMessage || undefined
        });
    });
})(window, document);
