/**
 * add.htmlのJavaScript
 */
document.addEventListener('DOMContentLoaded', function() {
    // DOM要素の取得
    const addActorButton = document.getElementById('add-actor-button');
    const actorsContainer = document.getElementById('actors-container');
    const addForm = document.getElementById('addForm'); // フォーム要素を取得

    let hasDuplicate = false; // 重複があったかどうかを追跡するフラグ

    // コンテナ内の全ての出演者名入力フィールドの値を取得する関数
    function getActorNames() {
        // actorsContainerがnullの場合に備える
        if (!actorsContainer) {
            console.error("actorsContainer element not found.");
            return [];
        }
        const inputs = actorsContainer.querySelectorAll('input[type="text"]');
        return Array.from(inputs).map(input => input.value.trim());
    }

    // 出演者名のリストに重複があるかどうかをチェックする関数
    function hasDuplicateActorNames() {
        const actorNames = getActorNames();
        const seen = new Set();
        for (const name of actorNames) {
            if (name !== '' && seen.has(name)) {
                return true; // 空欄以外の重複が見つかった場合
            }
            seen.add(name);
        }
        return false; // 重複無しの場合
    }

    // フォームの送信前に最終チェックを行う関数
    function validateAndSubmit(event) {
        hasDuplicate = hasDuplicateActorNames();
        if (hasDuplicate) {
            alert('同じ出演者が入力されています。登録しますか？');
            event.preventDefault(); // 重複があった場合は一旦送信を阻止
            return false; // 送信を阻止したことを示す
        }

        // 空の出演者フィールドを削除（送信前に）
        // actorsContainerがnullの場合に備える
        if (actorsContainer) {
            const actorDivs = actorsContainer.querySelectorAll('.actor-input-group');
            actorDivs.forEach(div => {
                const input = div.querySelector('input[type="text"]');
                // input.value.trim() のタイプミス (value,trim() -> value.trim()) を修正
                if (input && input.value.trim() === '' && actorDivs.length > 1) {
                    div.remove();
                }
            });
        }
        return true; // 送信を許可
    }

    // コンテナ内の最後のテキスト入力フィールドが空でないかを取得する関数
    function hasNonEmptyInputInLast() {
        // actorsContainerがnullの場合に備える
        if (!actorsContainer) {
            return true; // コンテナがなければ追加を許可
        }
        const inputs = actorsContainer.querySelectorAll('input[type="text"]');
        if (inputs.length > 0) {
            const lastInput = inputs[inputs.length - 1];
            return lastInput.value.trim() !== '';
        }
        return true; // 入力フィールドがまだない場合は true (追加を許可)
    }

    // 新しいテキストボックスを追加する関数
    function addActorField() {
        // actorsContainer が null でないことを確認する（安全のため）
        if (!actorsContainer) {
            console.error("actorsContainer element not found! Cannot add actor field.");
            return;
        }

        // 最初の入力フィールドがない場合、または最後の入力フィールドが空でない場合に追加
        if (hasNonEmptyInputInLast() || actorsContainer.querySelectorAll('input[type="text"]').length === 0) {
            const newActorInputGroup = document.createElement('div');
            newActorInputGroup.classList.add('actor-input-group'); // CSSクラスを維持

            const newInput = document.createElement('input');
            newInput.type = 'text';
            newInput.classList.add('form-input'); // CSSクラスを維持
            newInput.name = 'actors[]'; // 配列として送信するため
            newInput.placeholder = "出演者名";

            newActorInputGroup.appendChild(newInput);
            actorsContainer.appendChild(newActorInputGroup);
        }
    }

    // 「出演者を追加」ボタンのイベントリスナー設定
    if (addActorButton) {
        addActorButton.addEventListener('click', addActorField);
    } else {
        console.warn("Element with ID 'add-actor-button' not found. Add actor functionality might not work.");
    }

    // フォームの送信イベントリスナー設定（空フィールド削除と重複チェック）
    if (addForm) {
        addForm.addEventListener('submit', validateAndSubmit);
    } else {
        console.warn("Element with ID 'addForm' not found. Form submission validation might not work.");
    }
});

// DOMContentLoaded の外にある関数は、このJSファイルから呼び出されないため、
// 以下の関数は、HTMLのonclick属性などから直接呼び出される場合にのみ意味を持ちます。
// もしconfirmSaveがaddform.jsから呼び出されるなら、DOMContentLoaded内に入れるべきです。
// 通常、alertの「はい」でフォームを送信する場合はJSから関数を呼び出す形になるため、
// この関数は残しておきますが、どこから呼び出されているか確認が必要です。
function confirmSave() {
    const forceSaveInput = document.getElementById('forceSave');
    const addFormElement = document.getElementById('addForm');
    if (forceSaveInput) {
        forceSaveInput.value = 'true';
    } else {
        console.error("Element with ID 'forceSave' not found.");
    }
    if (addFormElement) {
        addFormElement.submit();
    } else {
        console.error("Element with ID 'addForm' not found for submission.");
    }
}