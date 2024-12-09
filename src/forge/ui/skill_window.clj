(ns ^:no-doc forge.ui.skill-window
  (:require [clojure.component :refer [defsystem]]))

(defsystem clicked-skillmenu-skill)
(defmethod clicked-skillmenu-skill :default [_ skill])

; TODO render text label free-skill-points
; (str "Free points: " (:entity/free-skill-points @player-eid))
#_(defn ->skill-window []
    (ui/window {:title "Skills"
                :id :skill-window
                :visible? false
                :cell-defaults {:pad 10}
                :rows [(for [id [:skills/projectile
                                 :skills/meditation
                                 :skills/spawn
                                 :skills/melee-attack]
                             :let [; get-property in callbacks if they get changed, this is part of context permanently
                                   button (image-button ; TODO reuse actionbar button scale?
                                                        (:entity/image (db/build id)) ; TODO here anyway taken
                                                        ; => should probably build this window @ game start
                                                        (fn []
                                                          (clicked-skillmenu-skill
                                                           (fsm/state-obj @player-eid)
                                                           (db/build id))))]]
                         (do
                          (add-tooltip! button #(info/->text (db/build id))) ; TODO no player modifiers applied (see actionbar)
                          button))]
                :pack? true}))
