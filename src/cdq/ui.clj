(ns cdq.ui)

(defprotocol Stage
  (get-ctx [_])
  (viewport-width [_])
  (viewport-height [_])
  (mouseover-actor [_ [x y]])
  (actor-information [_ actor])
  (action-bar-selected-skill [_])
  (rebuild-actors! [_ ctx])
  (inventory-window-visible? [_])
  (toggle-inventory-visible! [_])
  (show-modal-window! [_ ui-viewport {:keys [title text button-text on-click]}])
  (set-item! [_ cell item-properties])
  (remove-item! [_ inventory-cell])
  (add-skill! [_ skill-properties])
  (remove-skill! [_ skill-id])
  (show-text-message! [_ message])
  (toggle-entity-info-window! [_])
  (close-all-windows! [_])
  (show-error-window! [_ throwable]))

(defprotocol DataViewer
  (show-data-viewer! [_ data]))
