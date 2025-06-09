(ns cdq.create.world-event-handlers
  (:require [cdq.ctx]
            [cdq.state :as state]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.windows.inventory :as inventory-window]
            [cdq.ui.message]
            [cdq.utils :as utils]
            [gdl.audio :as audio]
            [gdl.graphics :as g]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]))

(defn- add-skill!
  [{:keys [ctx/graphics
           ctx/stage]}
   skill]
  (-> stage
      :action-bar
      (action-bar/add-skill! {:skill-id (:property/id skill)
                              :texture-region (g/image->texture-region graphics (:entity/image skill))
                              :tooltip-text #(cdq.ctx/info-text % skill) ; (assoc ctx :effect/source (world/player)) FIXME
                              }))
  nil)

(defn- remove-skill! [ctx skill]
  (-> ctx :ctx/stage :action-bar (action-bar/remove-skill! (:property/id skill)))
  nil)

(defn- set-item!
  [{:keys [ctx/graphics
           ctx/stage]
    :as ctx}
   [inventory-cell item]]
  (-> stage
      :windows
      :inventory-window
      (inventory-window/set-item! inventory-cell {:texture-region (g/image->texture-region graphics (:entity/image item))
                                                  :tooltip-text (cdq.ctx/info-text ctx item)})))

(defn- remove-item! [ctx inventory-cell]
  (-> ctx :ctx/stage :windows :inventory-window (inventory-window/remove-item! inventory-cell)))

(defn set-cursor! [{:keys [ctx/graphics]} cursor-key]
  (g/set-cursor! graphics cursor-key))

(defn player-state-changed [ctx new-state-obj]
  (when-let [cursor (state/cursor new-state-obj)]
    (set-cursor! ctx cursor)))

(defn play-sound! [{:keys [ctx/audio]} sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       (audio/play-sound! audio)))

(defn show-player-ui-msg! [{:keys [ctx/stage]} message]
  (-> stage
      (stage/find-actor "player-message")
      (cdq.ui.message/show! message)))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal-window! [{:keys [ctx/graphics
                                  ctx/stage]}
                          {:keys [title text button-text on-click]}]
  (assert (not (::modal stage)))
  (stage/add! stage
              (ui/window {:title title
                          :rows [[(ui/label text)]
                                 [(ui/text-button button-text
                                                  (fn [_actor _ctx]
                                                    (ui/remove! (::modal stage))
                                                    (on-click)))]]
                          :id ::modal
                          :modal? true
                          :center-position [(/ (:width (:ui-viewport graphics)) 2)
                                            (* (:height (:ui-viewport graphics)) (/ 3 4))]
                          :pack? true})))

(defn- toggle-inventory-visible! [{:keys [ctx/stage]} _]
  (-> stage :windows :inventory-window ui/toggle-visible!))

(defn do! [_ctx]
  {:world.event/player-skill-added add-skill!
   :world.event/player-skill-removed remove-skill!
   :world.event/player-item-set set-item!
   :world.event/player-item-removed remove-item!
   :world.event/player-state-changed player-state-changed
   :world.event/sound play-sound!
   :world.event/show-player-message show-player-ui-msg!
   :world.event/show-modal-window show-modal-window!
   :world.event/set-cursor set-cursor!
   :world.event/toggle-inventory-visible toggle-inventory-visible!})
