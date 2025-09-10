(ns cdq.txs)

(def txs-fn-map
  (let [sym-format "cdq.tx.%s/do!"
        ks [:tx/assoc
            :tx/assoc-in
            :tx/dissoc
            :tx/effect
            :tx/mark-destroyed
            :tx/mod-add
            :tx/mod-remove
            :tx/pay-mana-cost
            :tx/pickup-item
            :tx/set-cooldown
            :tx/add-text-effect
            :tx/add-skill
            :tx/player-add-skill
            :tx/set-item
            :tx/remove-item
            :tx/player-set-item
            :tx/player-remove-item
            :tx/event
            :tx/toggle-inventory-visible
            :tx/show-message
            :tx/show-modal
            :tx/sound
            :tx/state-exit
            :tx/state-enter
            :tx/audiovisual
            :tx/spawn-alert
            :tx/spawn-line
            :tx/deal-damage
            :tx/move-entity
            :tx/spawn-projectile
            :tx/spawn-effect
            :tx/spawn-item
            :tx/spawn-creature
            :tx/spawn-entity
            :tx/update-potential-fields
            ]]
    (into {}
          (for [k ks
                :let [sym (symbol (format sym-format (name k)))
                      f (requiring-resolve sym)]]
            (do
             (assert f (str "Cannot resolve " sym))
             [k f])))))
