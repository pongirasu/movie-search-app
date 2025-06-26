/*edit.html専用js

*/

document.addEventListener('DOMContentLoaded', function() {
    const actorsContainer = document.getElementById('actors-container'); // 出演者コンテナの要素を取得
    const addActorButton = document.getElementById('add-actor-button'); //「出演者を追加」ボタン要素を ID で取得
    const editForm = document.getElementById('editForm'); // 更新フォームを取得
    const initialActorsHidden = document.getElementById('initial-actors'); // 初期値取得用隠しフィールド
    const actualActorsInput = document.getElementById('actualActorsInput'); // 結合値送信用隠しフィールド
	
	// =======================================================
    // ページロード時の初期出演者入力フィールド生成
    // =======================================================
    if (initialActorsHidden && actualActorsInput) {
		const initialActorsString = initialActorsHidden.value;
		let actors = [];
		if (initialActorsString && initialActorsString.trim() != '') {
			// 複数の区切り文字を考慮して分割し、trimして空を除去
			actors = initialActorsString.split(/[/,、／\s]+/) // /や全角スラッシュ、カンマ、スペースなどで分割
													.map(actor => actor.trim())
													.filter(actor => actor != '');
		}
		
		// 初期出演者が要る場合はそれらを元にフィールドを生成
		if (actors.length > 0) {
			actors.forEach(actor => addActorField(actor)); // 既存の出演者毎にフィールドを追加
		} else {
			addActorField('') // 出演者がいない場合は１つの空のフィールドを追加
		}
		// 初期ロード後、すぐに隠しフィールドの値を更新
		updateActualActorsInput();
	} else {
		console.warn("Required elements for actor handling (initial-actors or actualActorsInput) not found.");
	}
    
    // =======================================================
    // ヘルパー関数 (既存関数を修正・強化)
    // =======================================================
    
    // コンテナ内の全ての出演者名入力フィールドの値を取得する関数
    function getActorNames() {
		// .actor-input クラスを持つ input[type="text"]を選択
        const inputs = actorsContainer.querySelectorAll('input.actor-input[type="text"]');
        return Array.from(inputs).map(input => input.value.trim());
    }

    // 出演者名のリストに重複があるかどうかをチェックする関数
    function hasDuplicateActorNames() {
        const actorNames = getActorNames().filter(name => name != ''); // 空欄は重複とみなさない
        const seen = new Set();
        for (const name of actorNames) {
            if (seen.has(name)) {
                return true; // 重複が見つかった
            }
            seen.add(name);
        }
        return false; // 重複なし
    }

    // 新しいテキストボックスを追加する条件（修正：初期値引数、削除ボタン追加）
    function addActorField(initialValue = '') {
		if (!actorsContainer) {
			console.error("actorsContainer element not found! Cannot add actor field.");
			return;
		}
        const newDiv = document.createElement('div');
        newDiv.classList.add('actor-input-group'); // グループ化のためのクラス
        
        const newInput = document.createElement('input');
		newInput.type = 'text';
        //newInput.name = 'actorsInput[]'; // JavaScriptが読み取るための名前
        newInput.classList.add('form-input', 'actor-input'); // CSSクラスと、JavaScriptが選択するためのクラス	
		newInput.placeholder = "出演者名"
		newInput.value = initialValue; // 初期値を設定
        
        const removeButton = document.createElement('button'); // 削除ボタンを追加
        removeButton.type = 'button';
        removeButton.textContent = '×'; // ボタンのテキスト
        removeButton.classList.add('remove-actor-button'); // スタイル用のクラス
        removeButton.addEventListener('click', function() {
			// 最後の１つは削除しない（中身をクリアする）
			if (actorsContainer.querySelectorAll('.actor-input-group').length > 1) {
				newDiv.remove();
			} else {
				newInput.value = ''; // 最後の１つの場合は中身をクリア
			}
			updateActualActorsInput();
		});
		
		newDiv.appendChild(newInput);
		newDiv.appendChild(removeButton); // 削除ボタンを追加
		actorsContainer.appendChild(newDiv);
		
		// 新しい入力フィールドが追加された際、入力値の変更を監視して隠しフィールドを更新
		newInput.addEventListener('input', updateActualActorsInput);
    }
    
    function updateActualActorsInput() {
		if (actualActorsInput) {
			const allActors = getActorNames();
			// 空欄を除外してから「／」で結合
			const combinedActors = allActors.filter(name => name != '').join('／');
			actualActorsInput.value = combinedActors;
		} else {
			console.error("Hidden input 'actualActorsInput' not found during update!");
		}
	}
    
	// =======================================================
    // イベントリスナー (修正・強化)
    // =======================================================
    
    // 「出演者を追加」ボタン
    if (addActorButton) {
        addActorButton.addEventListener('click', function() {
			// 最後の入力フィールドが空でなければ新しいフィールドを追加
			const inputs = actorsContainer.querySelectorAll('input.actor-input[type="text"]');
			if (inputs.length === 0 || inputs[inputs.length - 1].value.trim() !== '') {
				addActorField();
			}
			updateActualActorsInput();
		});
    }

    // フォームの送信イベントリスナー
    if (editForm) {
        editForm.addEventListener('submit', function(event) {
			// 1. フォーム送信直前に、必ず隠しフィールドを最終更新する
			updateActualActorsInput();
			
			// 2. 重複チェック
			if (hasDuplicateActorNames()) {
				// アラートを表示し、ユーザーに確認を求める
				// ここで 'confirm' を使って、ユーザーが「キャンセル」を選んだら送信を阻止する
				if (!confirm('同じ出演者が入力されています。このまま更新しますか？')) {
					event.preventDefault(); // ユーザーがキャンセルしたら送信を阻止
					return;
				}
			}
			// ここでフォームが送信される（重複チェックで阻止されなければ）
		});
    }
});