(ns cdq.ui.inventory)

(defprotocol Inventory
  (set-item! [_ cell {:keys [texture-region tooltip-text]}])
  (remove-item! [_ cell])
  (cell-with-item? [_ actor]))
