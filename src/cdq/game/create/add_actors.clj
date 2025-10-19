(ns cdq.game.create.add-actors
  (:require [cdq.game.create.hp-mana-bar-config :as hp-mana-bar-config]
            [cdq.game.create.entity-info-window-config :as entity-info-window-config]
            [cdq.game.create.inventory-window :as inventory-window]
            [cdq.graphics :as graphics]
            [cdq.input :as input]
            [cdq.ui :as ui]
            [cdq.ui.group :as build-group]
            [cdq.ui.message :as message]
            [cdq.ui.stage :as stage]
            [cdq.world.info :as info]
            [cdq.entity.state.player-item-on-cursor :as player-item-on-cursor]
            [clojure.gdx.scene2d.actor :as actor]
            [cdq.ui.info-window :as info-window]
            [cdq.ui.window :as window]))

(defn- create-hp-mana-bar* [create-draws]
  {:type :actor/actor
   :act (fn [_this _delta])
   :draw (fn [actor _batch _parent-alpha]
           (when-let [stage (actor/stage actor)]
             (graphics/draw! (:ctx/graphics (stage/ctx stage))
                             (create-draws (stage/ctx stage)))))})

(def state->draw-ui-view
  {:player-item-on-cursor (fn
                            [eid
                             {:keys [ctx/graphics
                                     ctx/input
                                     ctx/stage]}]
                            ; TODO see player-item-on-cursor at render layers
                            ; always draw it here at right position, then render layers does not need input/stage
                            ; can pass world to graphics, not handle here at application
                            (when (not (player-item-on-cursor/world-item? (ui/mouseover-actor stage (input/mouse-position input))))
                              [[:draw/texture-region
                                (graphics/texture-region graphics (:entity/image (:entity/item-on-cursor @eid)))
                                (:graphics/ui-mouse-position graphics)
                                {:center? true}]]))})

(defn- player-state-handle-draws
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [player-eid (:world/player-eid world)
        entity @player-eid
        state-k (:state (:entity/fsm entity))]
    (when-let [f (state->draw-ui-view state-k)]
      (graphics/draw! graphics (f player-eid ctx)))))

(def message-duration-seconds 0.5)

(declare step)

(defn rebuild-actors! [stage ctx]
  (stage/clear! stage)
  (step stage ctx))

(defn step [stage ctx]
  (let [config (.config stage)]
    (doseq [actor [((:dev-menu config)
                    ctx
                    rebuild-actors!
                    (requiring-resolve 'cdq.game.create.world/step)
                    (requiring-resolve 'cdq.game.open-editor/do!))
                   {:type :actor/action-bar}
                   (create-hp-mana-bar* (hp-mana-bar-config/create ctx))
                   (build-group/create
                    {:actor/name "cdq.ui.windows"
                     :group/actors [(info-window/create (entity-info-window-config/create ctx))
                                    (inventory-window/create ctx)]})
                   (actor/create
                    {:draw (fn [this _batch _parent-alpha]
                             (player-state-handle-draws (stage/ctx (actor/stage this))))
                     :act (fn [this _delta])})
                   (message/create message-duration-seconds)]]
      (stage/add-actor! stage actor))))
