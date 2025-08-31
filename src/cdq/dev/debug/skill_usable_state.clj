(comment

  (defn debug-applicable-check [effects effect-ctx]
    (map (fn [[k v]]
           [k (cdq.world.effect/applicable? [k v] effect-ctx)])
         effects))

 (let [ctx @cdq.application/state
       eid (:world/player-eid (:ctx/world ctx))
       effect-ctx (ctx/player-effect-ctx ctx eid)
       selected-skill (action-bar/selected-skill (:action-bar (:ctx/stage ctx))) ; TODO ID !!!
       skill (selected-skill (:entity/skills @eid))
       usable-state (entity/skill-usable-state @eid skill effect-ctx)
       ]
   (clojure.pprint/pprint
    {
     :effect-ctx (map (fn [[k v]] [k (class v)]) effect-ctx)
     ;:selected-skill skill
     :usable-state usable-state
     :effects (:skill/effects skill)
     :effects-applicable (map (fn [[k v]]
                                [k (cdq.world.effect/applicable? [k v] effect-ctx)])
                              (:skill/effects skill))
     })
   )

 (clojure.pprint/pprint
  (:skill/effects (cdq.ctx.db/get-raw (:ctx/db @cdq.application/state)
                                  :skills/melee-attack
                                  )))
 )
