(ns cdq.c
  (:require cdq.application.create.editor
            cdq.ui.editor.window
            [cdq.application.create.db]
            [cdq.application.create.vis-ui]
            [cdq.application.create.graphics]
            [cdq.application.create.stage]
            [cdq.application.create.input]
            [cdq.application.create.audio]
            [cdq.application.create.remove-files]
            [cdq.application.create.world]
            [cdq.application.create.reset-game-state]
            [cdq.ctx :as ctx]
            [cdq.malli :as m]
            [gdl.tx-handler :as tx-handler]
            [qrecord.core :as q]))

(def ^:private schema
  (m/schema
   [:map {:closed true}
    [:ctx/audio :some]
    [:ctx/db :some]
    [:ctx/graphics :some]
    [:ctx/input :some]
    [:ctx/stage :some]
    [:ctx/vis-ui :some]
    [:ctx/world :some]]))

(def ^:private txs-fn-map
  '{
    :tx/sound cdq.tx.sound/do!

    :tx/assoc (fn [_ctx eid k value]
                (swap! eid assoc k value)
                nil)

    :tx/assoc-in (fn [_ctx eid ks value]
                   (swap! eid assoc-in ks value)
                   nil)

    :tx/dissoc (fn [_ctx eid k]
                 (swap! eid dissoc k)
                 nil)
    :tx/mark-destroyed (fn [_ctx eid]
                         (swap! eid assoc :entity/destroyed? true)
                         nil)
    :tx/mod-add cdq.tx.mod-add/do!
    :tx/mod-remove cdq.tx.mod-remove/do!
    :tx/pay-mana-cost cdq.tx.pay-mana-cost/do!
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

    :tx/print-stacktrace cdq.tx.print-stacktrace/do!

    :tx/show-error-window        cdq.tx.stage/show-error-window!
    :tx/toggle-inventory-visible cdq.tx.stage/toggle-inventory-visible!
    :tx/show-message             cdq.tx.stage/show-message!
    :tx/show-modal               cdq.tx.stage/show-modal!
    :tx/audiovisual cdq.tx.audiovisual/do!

    :tx/spawn-alert cdq.tx.spawn-alert/do!
    :tx/spawn-line cdq.tx.spawn-line/do!
    :tx/move-entity cdq.tx.move-entity/do!
    :tx/spawn-projectile cdq.tx.spawn-projectile/do!
    :tx/spawn-effect cdq.tx.spawn-effect/do!
    :tx/spawn-item     cdq.tx.spawn-item/do!
    :tx/spawn-creature cdq.tx.spawn-creature/do!
    :tx/spawn-entity   cdq.tx.spawn-entity/do!
    }
  )

(alter-var-root #'txs-fn-map update-vals
                (fn [form]
                  (if (symbol? form)
                    (let [avar (requiring-resolve form)]
                      (assert avar form)
                      avar)
                    (eval form))))


(require 'cdq.tx.stage)

(def ^:private reaction-txs-fn-map
  {

   :tx/set-item (fn [ctx eid cell item]
                  (when (:entity/player? @eid)
                    (cdq.tx.stage/player-set-item! ctx cell item)
                    nil))

   :tx/remove-item (fn [ctx eid cell]
                     (when (:entity/player? @eid)
                       (cdq.tx.stage/player-remove-item! ctx cell)
                       nil))

   :tx/add-skill (fn [ctx eid skill]
                   (when (:entity/player? @eid)
                     (cdq.tx.stage/player-add-skill! ctx skill)
                     nil))
   }
  )

(q/defrecord Context []
  ctx/Validation
  (validate [ctx]
    (m/validate-humanize schema ctx)
    ctx)

  ctx/TransactionHandler
  (handle-txs! [ctx transactions]
    (let [handled-txs (tx-handler/actions!
                       txs-fn-map
                       ctx  ; here pass only world ....
                       transactions)]
      (tx-handler/actions!
       reaction-txs-fn-map
       ctx
       handled-txs
       :strict? false))))

(defn- create-record [ctx]
  (merge (map->Context {})
         ctx))

(defn create! [ctx]
  (-> ctx
      create-record
      cdq.application.create.db/do!
      cdq.application.create.vis-ui/do!
      cdq.application.create.graphics/do!
      cdq.application.create.stage/do!
      cdq.application.create.input/do!
      cdq.application.create.audio/do!
      cdq.application.create.remove-files/do!
      cdq.application.create.world/do!
      cdq.application.create.reset-game-state/do!))
