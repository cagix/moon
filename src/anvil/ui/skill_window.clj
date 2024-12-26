(ns ^:no-doc anvil.ui.skill-window
  (:require [anvil.component :refer [clicked-skillmenu-skill]]
            [anvil.entity.skills :as skills]))

(defmethod clicked-skillmenu-skill :player-idle [[_ {:keys [eid]}] skill c]
  (let [free-skill-points (:entity/free-skill-points @eid)]
    ; TODO no else case, no visible free-skill-points
    (when (and (pos? free-skill-points)
               (not (skills/contains? @eid skill)))
      (swap! eid assoc :entity/free-skill-points (dec free-skill-points))
      (skills/add c eid skill))))

; TODO render text label free-skill-points
; (str "Free points: " (:entity/free-skill-points @world/player-eid))
#_(defn ->skill-window [{:keys [cdq.context/player-eid] :as c}]
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
                                                        (:entity/image (db/build c id)) ; TODO here anyway taken
                                                        ; => should probably build this window @ game start
                                                        (fn []
                                                          (clicked-skillmenu-skill
                                                           (entity/state-obj @player-eid)
                                                           (db/build c id)
                                                           c)))]]
                         (do
                          (add-tooltip! button #(info/text @gdl.app/state
                                                           (db/build c id))) ; TODO no player modifiers applied (see actionbar)
                          button))]
                :pack? true}))
