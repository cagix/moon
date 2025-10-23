(ns cdq.ctx
  (:require [cdq.audio :as audio]
            [cdq.db.impl]
            ;;
            [cdq.game.create.add-actors]
            [cdq.game.create.dev-menu-config]
            [cdq.game.create.world]
            [cdq.game.render.assoc-active-entities]
            [cdq.game.render.assoc-interaction-state]
            [cdq.game.render.assoc-paused]
            [cdq.game.render.check-open-debug]
            [cdq.game.render.clear-screen]
            [cdq.game.render.dissoc-interaction-state]
            [cdq.game.render.draw-on-world-viewport]
            [cdq.game.render.draw-world-map]
            [cdq.game.render.get-stage-ctx]
            [cdq.game.render.player-state-handle-input]
            [cdq.game.render.remove-destroyed-entities]
            [cdq.game.render.render-stage]
            [cdq.game.render.set-camera-on-player]
            [cdq.game.render.set-cursor]
            [cdq.game.render.tick-entities]
            [cdq.game.render.update-mouse]
            [cdq.game.render.update-mouseover-eid]
            [cdq.game.render.update-potential-fields]
            [cdq.game.render.update-world-time]
            [cdq.game.render.validate]
            [cdq.game.render.validate]
            [cdq.game.render.window-camera-controls]
            ;;
            [cdq.graphics :as graphics]
            [cdq.graphics.impl]
            [cdq.ui :as ui]
            [cdq.ui.build.editor-window]
            [cdq.ui.dev-menu]
            [cdq.ui.editor.overview-window]
            [cdq.ui.editor.window]
            [cdq.ui.impl]
            [cdq.world :as world]
            [cdq.world.impl]
            [clojure.tx-handler :as tx-handler]
            [clojure.txs :as txs]
            [qrecord.core :as q]))

(def reaction-txs-fn-map
  (update-vals '{
                 :tx/sound                    cdq.tx.sound/do!
                 :tx/toggle-inventory-visible cdq.tx.toggle-inventory-visible/do!
                 :tx/show-message             cdq.tx.show-message/do!
                 :tx/show-modal               cdq.tx.show-modal/do!
                 :tx/set-item                 cdq.tx.set-item/do!
                 :tx/remove-item              cdq.tx.remove-item/do!
                 :tx/add-skill                cdq.tx.add-skill/do!
                 }
               requiring-resolve))

(def txs-fn-map
  (update-vals '{
                 ;; FIXME only this passes ctx, otherwise 'world' only
                 :tx/state-exit               cdq.world.tx.state-exit/do!
                 :tx/audiovisual              cdq.world.tx.audiovisual/do!
                 ;;

                 :tx/assoc                    cdq.world.tx.assoc/do!
                 :tx/assoc-in                 cdq.world.tx.assoc-in/do!
                 :tx/dissoc                   cdq.world.tx.dissoc/do!
                 :tx/update                   cdq.world.tx.update/do!
                 :tx/mark-destroyed           cdq.world.tx.mark-destroyed/do!
                 :tx/set-cooldown             cdq.world.tx.set-cooldown/do!
                 :tx/add-text-effect          cdq.world.tx.add-text-effect/do!
                 :tx/add-skill                cdq.world.tx.add-skill/do!
                 :tx/set-item                 cdq.world.tx.set-item/do!
                 :tx/remove-item              cdq.world.tx.remove-item/do!
                 :tx/pickup-item              cdq.world.tx.pickup-item/do!
                 :tx/event                    cdq.world.tx.event/do!
                 :tx/state-enter              cdq.world.tx.state-enter/do!
                 :tx/effect                   cdq.world.tx.effect/do!
                 :tx/spawn-alert              cdq.world.tx.spawn-alert/do!
                 :tx/spawn-line               cdq.world.tx.spawn-line/do!
                 :tx/move-entity              cdq.world.tx.move-entity/do!
                 :tx/spawn-projectile         cdq.world.tx.spawn-projectile/do!
                 :tx/spawn-effect             cdq.world.tx.spawn-effect/do!
                 :tx/spawn-item               cdq.world.tx.spawn-item/do!
                 :tx/spawn-creature           cdq.world.tx.spawn-creature/do!
                 :tx/spawn-entity             cdq.world.tx.spawn-entity/do!
                 :tx/sound                    cdq.world.tx.nothing/do!
                 :tx/toggle-inventory-visible cdq.world.tx.nothing/do!
                 :tx/show-message             cdq.world.tx.nothing/do!
                 :tx/show-modal               cdq.world.tx.nothing/do!
                 }
               requiring-resolve))

(q/defrecord Context []
  txs/TransactionHandler
  (handle! [ctx txs]
    (let [handled-txs (tx-handler/actions! txs-fn-map ctx txs)]
      (tx-handler/actions! reaction-txs-fn-map
                           ctx
                           handled-txs
                           :strict? false))))

(defn create!
  [{:keys [audio
           files
           graphics
           input]}
   config]
  (let [graphics (cdq.graphics.impl/create! graphics files (:graphics config))
        stage (cdq.ui.impl/create! graphics {:dev-menu cdq.game.create.dev-menu-config/create})
        ctx (-> (map->Context {})
                (assoc :ctx/graphics graphics)
                (assoc :ctx/stage stage)
                (assoc :ctx/audio (cdq.audio/create audio files (:audio config)))
                (assoc :ctx/db (cdq.db.impl/create))
                (assoc :ctx/input input)
                (assoc :ctx/config {:world-impl cdq.world.impl/create}))]
    (.setInputProcessor input stage)
    (cdq.game.create.add-actors/step stage ctx)
    (cdq.game.create.world/step ctx (:world config))))

(defn dispose!
  [{:keys [ctx/audio
           ctx/graphics
           ctx/stage
           ctx/world]}]
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (ui/dispose! stage)
  (world/dispose! world))

(defn render! [ctx]
  (-> ctx
      cdq.game.render.get-stage-ctx/step
      cdq.game.render.validate/step
      cdq.game.render.update-mouse/step
      cdq.game.render.update-mouseover-eid/step
      cdq.game.render.check-open-debug/step
      cdq.game.render.assoc-active-entities/step
      cdq.game.render.set-camera-on-player/step
      cdq.game.render.clear-screen/step
      cdq.game.render.draw-world-map/step
      cdq.game.render.draw-on-world-viewport/step
      cdq.game.render.assoc-interaction-state/step
      cdq.game.render.set-cursor/step
      cdq.game.render.player-state-handle-input/step
      cdq.game.render.dissoc-interaction-state/step
      cdq.game.render.assoc-paused/step
      cdq.game.render.update-world-time/step
      cdq.game.render.update-potential-fields/step
      cdq.game.render.tick-entities/step
      cdq.game.render.remove-destroyed-entities/step
      cdq.game.render.window-camera-controls/step
      cdq.game.render.render-stage/step
      cdq.game.render.validate/step))

(defn resize! [{:keys [ctx/graphics]} width height]
  (graphics/update-ui-viewport! graphics width height)
  (graphics/update-world-vp! graphics width height))
