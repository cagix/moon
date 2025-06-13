(ns cdq.ui)

(defprotocol ActionBar
  (selected-skill [_]))


(comment

 (require '[cdq.ui.action-bar :as action-bar])

 (extend-type gdl.ui.CtxStage
   ActionBar
   (selected-skill [stage]
     (action-bar/selected-skill (:action-bar stage))))

 (let [stage (:ctx/stage @cdq.application/state)]
   (selected-skill stage))

 )
