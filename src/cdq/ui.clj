(ns cdq.ui)

(defprotocol ActionBar
  (selected-skill [_]))

(require '[cdq.ui.action-bar :as action-bar])

(comment
 (extend-type gdl.ui.CtxStage
   ActionBar
   (selected-skill [stage]
     (action-bar/selected-skill (:action-bar stage)))
   )
 (let [stage (:ctx/stage @cdq.application/state)]
   (selected-skill stage)))
