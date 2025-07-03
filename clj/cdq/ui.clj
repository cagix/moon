(ns cdq.ui)

(defprotocol Stage
  (action-bar-selected-skill [_])
  (add-action-bar-skill! [_ item-opts])
  (remove-action-bar-skill! [_ id])
  (set-inventory-item! [_ inventory-cell item-opts])
  (remove-inventory-item! [_ inventory-cell])
  (show-player-ui-msg! [_ message])
  (show-modal-window! [_ ui-viewport {:keys [title text button-text on-click]}])
  (toggle-inventory-visible! [_])
  (toggle-entity-info-window! [_])
  (close-all-windows! [_]))
