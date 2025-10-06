(ns cdq.ctx.create.handle-txs
  (:require [cdq.audio :as sounds]
            [cdq.graphics.textures :as textures]
            [cdq.ui :as ui]
            [clojure.info :as info]
            [clojure.scene2d :as scene2d]
            [cdq.ui.stage :as stage]
            [clojure.tx-handler :as tx-handler]
            [clojure.txs :as txs]))

(defn- player-add-skill!
  [{:keys [ctx/graphics
           ctx/stage]}
   skill]
  (ui/add-skill! stage
                 {:skill-id (:property/id skill)
                  :texture-region (textures/texture-region graphics (:entity/image skill))
                  :tooltip-text (fn [{:keys [ctx/world]}]
                                  (info/text skill world))})
  nil)

(defn- player-set-item!
  [{:keys [ctx/graphics
           ctx/stage]}
   cell item]
  (ui/set-item! stage cell
                {:texture-region (textures/texture-region graphics (:entity/image item))
                 :tooltip-text (info/text item nil)})
  nil)

(defn player-remove-item! [{:keys [ctx/stage]}
                           cell]
  (ui/remove-item! stage cell)
  nil)

(defn toggle-inventory-visible! [{:keys [ctx/stage]}]
  (ui/toggle-inventory-visible! stage)
  nil)

(defn show-message! [{:keys [ctx/stage]} message]
  (ui/show-text-message! stage message)
  nil)

(defn show-modal! [{:keys [ctx/stage]} opts]
  (ui/show-modal-window! stage (stage/viewport stage) opts)
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
                (sounds/play! audio sound-name)
                nil)
    :tx/toggle-inventory-visible cdq.ctx.create.handle-txs/toggle-inventory-visible!
    :tx/show-message             cdq.ctx.create.handle-txs/show-message!
    :tx/show-modal               cdq.ctx.create.handle-txs/show-modal!
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

(defn do! [ctx]
  (extend-type (class ctx)
    txs/TransactionHandler
    (handle! [ctx txs]
      (let [handled-txs (tx-handler/actions! txs-fn-map
                                             ctx
                                             txs)]
        (tx-handler/actions! reaction-txs-fn-map
                             ctx
                             handled-txs
                             :strict? false))))
  ctx)
