document.addEventListener('DOMContentLoaded', function () {
    const hash = window.location.hash || '';
    if (hash.startsWith('#comment-')) {
        const target = document.querySelector(hash);
        if (target) {
            target.classList.add('comment-focus');
            setTimeout(function () {
                target.classList.remove('comment-focus');
            }, 2200);
        }
    }

    const shareButton = document.getElementById('shareBoardButton');
    if (shareButton) {
        shareButton.addEventListener('click', async function () {
            const rawSharePath = shareButton.getAttribute('data-share-url') || window.location.pathname;
            const shareUrl = new URL(rawSharePath, window.location.origin).toString();
            const shareData = {
                title: document.title || '게시글 공유',
                url: shareUrl
            };

            if (navigator.share) {
                try {
                    await navigator.share(shareData);
                    return;
                } catch (ignored) {
                }
            }

            try {
                await navigator.clipboard.writeText(shareUrl);
                window.alert('게시글 링크가 복사되었습니다.');
            } catch (ignored) {
                window.prompt('아래 링크를 복사하세요.', shareUrl);
            }
        });
    }

    const reportModal = document.getElementById('reportModal');
    if (reportModal) {
        const reportReason = reportModal.querySelector('#reportReason');
        const reportDetails = reportModal.querySelector('#reportDetails');
        const reportForm = reportModal.querySelector('form');

        function syncReportDetailsRequirement() {
            if (!reportReason || !reportDetails) {
                return;
            }
            const needDetails = reportReason.value === 'OTHER';
            reportDetails.required = needDetails;
            reportDetails.placeholder = needDetails
                ? '기타 사유는 상세 설명을 반드시 입력해주세요. (최대 500자)'
                : '추가 설명이 필요하면 입력해주세요. (최대 500자)';
        }

        if (reportReason && reportDetails) {
            reportReason.addEventListener('change', syncReportDetailsRequirement);
            syncReportDetailsRequirement();
        }

        if (reportForm && reportReason && reportDetails) {
            reportForm.addEventListener('submit', function (event) {
                if (reportReason.value === 'OTHER' && !reportDetails.value.trim()) {
                    event.preventDefault();
                    window.alert('기타 사유는 상세 내용을 입력해주세요.');
                    reportDetails.focus();
                }
            });
        }
    }

    const contentRoot = document.querySelector('.board-detail-content');
    if (!contentRoot) {
        return;
    }

    const images = contentRoot.querySelectorAll('img[src]');
    const localImagePaths = [];

    images.forEach(function (img) {
        const rawSrc = (img.getAttribute('src') || '').trim();
        if (!rawSrc) {
            return;
        }

        let pathName = '';
        try {
            pathName = new URL(rawSrc, window.location.origin).pathname || '';
        } catch (ignored) {
            return;
        }

        if (!pathName.startsWith('/images/')) {
            return;
        }

        const originalViewUrl = '/api/images/original?url=' + encodeURIComponent(pathName);
        localImagePaths.push(pathName);

        img.style.cursor = 'zoom-in';
        img.title = '원본 이미지 새 탭 열기';
        img.addEventListener('click', function (event) {
            event.preventDefault();
            event.stopPropagation();
            window.open(originalViewUrl, '_blank', 'noopener,noreferrer');
        });
    });

    if (new Set(localImagePaths).size > 0) {
        const boardIdMatch = window.location.pathname.match(/^\/boards\/(\d+)(?:\/.*)?$/);
        if (boardIdMatch && boardIdMatch[1]) {
            const bulkDownloadWrapper = document.createElement('div');
            bulkDownloadWrapper.className = 'p-4 border-bottom text-end';

            const bulkDownloadLink = document.createElement('a');
            bulkDownloadLink.className = 'btn btn-sm btn-outline-primary';
            bulkDownloadLink.href = '/api/images/boards/' + encodeURIComponent(boardIdMatch[1]) + '/zip';
            bulkDownloadLink.textContent = '이미지 일괄 다운로드 (ZIP)';

            bulkDownloadWrapper.appendChild(bulkDownloadLink);
            contentRoot.insertAdjacentElement('afterend', bulkDownloadWrapper);
        }
    }
});
