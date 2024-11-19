(ns ^:no-doc moon.widgets.skill-window)

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
                                   button (ui/image-button ; TODO reuse actionbar button scale?
                                                           (:entity/image (db/get id)) ; TODO here anyway taken
                                                           ; => should probably build this window @ game start
                                                           (fn []
                                                             (tx/do! (player-clicked-skillmenu (db/get id)))))]]
                         (do
                          (ui/add-tooltip! button #(info/->text (db/get id))) ; TODO no player modifiers applied (see actionbar)
                          button))]
                :pack? true}))
