(ns cdq.create.world-event-handlers
  (:require [cdq.ctx :as ctx]
            [cdq.ui.stage :as stage]
            [gdl.audio :as audio]
            [gdl.graphics :as g]))

(defn- add-skill!
  [{:keys [ctx/graphics
           ctx/stage]}
   skill]
  (stage/add-action-bar-skill! stage
                               {:skill-id (:property/id skill)
                                :texture-region (g/image->texture-region graphics (:entity/image skill))
                                ; (assoc ctx :effect/source (world/player)) FIXME
                                :tooltip-text #(ctx/info-text % skill)})
  nil)

(defn- remove-skill! [{:keys [ctx/stage]} skill]
  (stage/remove-action-bar-skill! stage (:property/id skill))
  nil)

(defn- set-item!
  [{:keys [ctx/graphics
           ctx/stage]
    :as ctx}
   [inventory-cell item]]
  (stage/set-inventory-item! stage
                             inventory-cell
                             {:texture-region (g/image->texture-region graphics (:entity/image item))
                              :tooltip-text (ctx/info-text ctx item)}))

(defn- remove-item! [{:keys [ctx/stage]} inventory-cell]
  (stage/remove-inventory-item! stage inventory-cell))

(defn play-sound! [{:keys [ctx/audio]} sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       (audio/play-sound! audio)))

(defn show-player-ui-msg! [{:keys [ctx/stage]} message]
  (stage/show-player-ui-msg! stage message))

(defn show-modal-window! [{:keys [ctx/graphics
                                  ctx/stage]}
                          opts]
  (stage/show-modal-window! stage
                            (:ui-viewport graphics)
                            opts))

(defn- toggle-inventory-visible! [{:keys [ctx/stage]} _]
  (stage/toggle-inventory-visible! stage))

(defn do! [_ctx _params]
  {:world.event/player-skill-added add-skill!
   :world.event/player-skill-removed remove-skill!
   :world.event/player-item-set set-item!
   :world.event/player-item-removed remove-item!
   :world.event/sound play-sound!
   :world.event/show-player-message show-player-ui-msg!
   :world.event/show-modal-window show-modal-window!
   :world.event/toggle-inventory-visible toggle-inventory-visible!})
