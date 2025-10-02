(ns cdq.ui)

(defprotocol Stage
  (mouseover-actor [_ [x y]])
  (actor-information [_ actor])
  (action-bar-selected-skill [_])
  (rebuild-actors! [_ ctx]))
