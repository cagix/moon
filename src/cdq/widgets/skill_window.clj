(ns cdq.widgets.skill-window)

(defmulti clicked-skillmenu-skill (fn [[k] skill c]
                                    k))
(defmethod clicked-skillmenu-skill :default [_ skill c])

#_(defmethod state/clicked-skillmenu-skill :player-idle
  [[_ {:keys [eid]}] skill c]
  (let [free-skill-points (:entity/free-skill-points @eid)]
    ; TODO no else case, no visible free-skill-points
    (when (and (pos? free-skill-points)
               (not (entity/has-skill? @eid skill)))
      (swap! eid assoc :entity/free-skill-points (dec free-skill-points))
      (tx/add-skill eid skill))))

; TODO render text label free-skill-points
; (str "Free points: " (:entity/free-skill-points @world/player-eid))
#_(defn ->skill-window [c]
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
                                                        (fn [context]
                                                          (state/clicked-skillmenu-skill
                                                           (entity/state-obj @world/player-eid)
                                                           (db/build id)
                                                           c)))]]
                         (do
                          (add-tooltip! button #(info/text % (db/build id))) ; TODO no player modifiers applied (see actionbar)
                          button))]
                :pack? true}))
