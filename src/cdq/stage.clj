(ns cdq.stage)

(defprotocol Stage
  (toggle-inventory-visible! [stage])
  (show-modal-window! [stage ui-viewport {:keys [title text button-text on-click]}])
  (set-item!  [stage cell item-properties])
  (remove-item!  [stage inventory-cell])
  (add-skill!  [stage skill-properties])
  (remove-skill!  [stage skill-id])
  (show-text-message!  [stage message]))
