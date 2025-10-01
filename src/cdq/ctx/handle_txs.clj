(ns cdq.ctx.handle-txs
  (:require [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.info :as info]
            [cdq.stage]
            [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [gdl.tx-handler :as tx-handler]))

(defn- player-add-skill!
  [{:keys [ctx/graphics
           ctx/stage]}
   skill]
  (cdq.stage/add-skill! stage
                        {:skill-id (:property/id skill)
                         :texture-region (graphics/texture-region graphics (:entity/image skill))
                         :tooltip-text (fn [{:keys [ctx/world]}]
                                         (info/info-text skill world))})
  nil)

(defn- player-set-item! [{:keys [ctx/graphics
                                ctx/stage]}
                        cell item]
  (cdq.stage/set-item! stage cell
                       {:texture-region (graphics/texture-region graphics (:entity/image item))
                        :tooltip-text (fn [{:keys [ctx/world]}]
                                        (info/info-text item world))})
  nil)

(defn player-remove-item! [{:keys [ctx/stage]}
                           cell]
  (cdq.stage/remove-item! stage cell)
  nil)

(defn toggle-inventory-visible! [{:keys [ctx/stage]}]
  (cdq.stage/toggle-inventory-visible! stage)
  nil)

(defn show-message! [{:keys [ctx/stage]} message]
  (cdq.stage/show-text-message! stage message)
  nil)

(defn show-modal! [{:keys [ctx/stage]} opts]
  (cdq.stage/show-modal-window! stage (stage/viewport stage) opts)
  nil)

(def ^:private txs-fn-map
  '{
    :tx/assoc (fn [_ctx eid k value]
                (swap! eid assoc k value)
                nil)
    :tx/assoc-in (fn [_ctx eid ks value]
                   (swap! eid assoc-in ks value)
                   nil)
    :tx/dissoc (fn [_ctx eid k]
                 (swap! eid dissoc k)
                 nil)
    :tx/update (fn [_ctx eid & params]
                 (apply swap! eid update params)
                 nil)
    :tx/mark-destroyed (fn [_ctx eid]
                         (swap! eid assoc :entity/destroyed? true)
                         nil)
    :tx/set-cooldown cdq.tx.set-cooldown/do!
    :tx/add-text-effect cdq.tx.add-text-effect/do!
    :tx/add-skill cdq.tx.add-skill/do!
    :tx/set-item cdq.tx.set-item/do!
    :tx/remove-item cdq.tx.remove-item/do!
    :tx/pickup-item cdq.tx.pickup-item/do!
    :tx/event cdq.tx.event/do!
    :tx/state-exit cdq.tx.state-exit/do!
    :tx/state-enter cdq.tx.state-enter/do!
    :tx/effect cdq.tx.effect/do!
    :tx/audiovisual cdq.tx.audiovisual/do!
    :tx/spawn-alert cdq.tx.spawn-alert/do!
    :tx/spawn-line cdq.tx.spawn-line/do!
    :tx/move-entity cdq.tx.move-entity/do!
    :tx/spawn-projectile cdq.tx.spawn-projectile/do!
    :tx/spawn-effect cdq.tx.spawn-effect/do!
    :tx/spawn-item     cdq.tx.spawn-item/do!
    :tx/spawn-creature cdq.tx.spawn-creature/do!
    :tx/spawn-entity   cdq.tx.spawn-entity/do!

    :tx/sound (fn [{:keys [ctx/audio]} sound-name]
                (audio/play-sound! audio sound-name)
                nil)
    :tx/toggle-inventory-visible cdq.ctx.handle-txs/toggle-inventory-visible!
    :tx/show-message             cdq.ctx.handle-txs/show-message!
    :tx/show-modal               cdq.ctx.handle-txs/show-modal!
    }
  )

(alter-var-root #'txs-fn-map update-vals
                (fn [form]
                  (if (symbol? form)
                    (let [avar (requiring-resolve form)]
                      (assert avar form)
                      avar)
                    (eval form))))

(def ^:private reaction-txs-fn-map
  {

   :tx/set-item (fn [ctx eid cell item]
                  (when (:entity/player? @eid)
                    (player-set-item! ctx cell item)
                    nil))

   :tx/remove-item (fn [ctx eid cell]
                     (when (:entity/player? @eid)
                       (player-remove-item! ctx cell)
                       nil))

   :tx/add-skill (fn [ctx eid skill]
                   (when (:entity/player? @eid)
                     (player-add-skill! ctx skill)
                     nil))
   }
  )

(defn do! [ctx transactions]
  (let [handled-txs (tx-handler/actions!
                     txs-fn-map
                     ctx
                     transactions)]
    (tx-handler/actions!
     reaction-txs-fn-map
     ctx
     handled-txs
     :strict? false))
  ctx)
