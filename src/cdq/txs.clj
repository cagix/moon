(ns cdq.txs)

(def txs-fn-map
  (into {}
        (for [[k sym] '{:tx/assoc cdq.tx.assoc/do!
                        :tx/assoc-in cdq.tx.assoc-in/do!
                        :tx/dissoc cdq.tx.dissoc/do!
                        :tx/effect cdq.tx.effect/do!
                        :tx/mark-destroyed cdq.tx.mark-destroyed/do!
                        :tx/mod-add cdq.tx.mod-add/do!
                        :tx/mod-remove cdq.tx.mod-remove/do!
                        :tx/pay-mana-cost cdq.tx.pay-mana-cost/do!
                        :tx/pickup-item cdq.tx.pickup-item/do!
                        :tx/set-cooldown cdq.tx.set-cooldown/do!
                        :tx/add-text-effect cdq.tx.add-text-effect/do!
                        :tx/add-skill cdq.tx.add-skill/do!
                        :tx/player-add-skill cdq.tx.player-add-skill/do!
                        :tx/set-item cdq.tx.set-item/do!
                        :tx/remove-item cdq.tx.remove-item/do!
                        :tx/player-set-item cdq.tx.player-set-item/do!
                        :tx/player-remove-item cdq.tx.player-remove-item/do!
                        :tx/event cdq.tx.event/do!
                        :tx/toggle-inventory-visible cdq.tx.toggle-inventory-visible/do!
                        :tx/show-message cdq.tx.show-message/do!
                        :tx/show-modal cdq.tx.show-modal/do!
                        :tx/sound cdq.tx.sound/do!
                        :tx/state-exit cdq.tx.state-exit/do!
                        :tx/state-enter cdq.tx.state-enter/do!
                        :tx/audiovisual cdq.tx.audiovisual/do!
                        :tx/spawn-alert cdq.tx.spawn-alert/do!
                        :tx/spawn-line cdq.tx.spawn-line/do!
                        :tx/deal-damage cdq.tx.deal-damage/do!
                        :tx/move-entity cdq.tx.move-entity/do!
                        :tx/spawn-projectile cdq.tx.spawn-projectile/do!
                        :tx/spawn-effect cdq.tx.spawn-effect/do!
                        :tx/spawn-item cdq.tx.spawn-item/do!
                        :tx/spawn-creature cdq.tx.spawn-creature/do!
                        :tx/spawn-entity cdq.tx.spawn-entity/do!}
              :let [f (requiring-resolve sym)]]
          (do
           (assert f (str "Cannot resolve " sym))
           [k f]))))
