(ns cdq.ui)

(defprotocol Stage
  (get-ctx [_])
  (viewport-width [_])
  (viewport-height [_])
  (mouseover-actor [_ [x y]])
  (actor-information [_ actor])
  (action-bar-selected-skill [_])
  (rebuild-actors! [_ ctx]))

(defprotocol DataViewer
  (show-data-viewer! [_ data]))
