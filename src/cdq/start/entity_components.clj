(ns cdq.start.entity-components)

(def method-mappings
  '{:entity/animation                       {:create   cdq.entity.animation/create}
    :entity/body                            {:create   cdq.entity.body/create}
    :entity/delete-after-animation-stopped? {:create!  cdq.entity.delete-after-animation-stopped/create!}
    :entity/delete-after-duration           {:create   cdq.entity.delete-after-duration/create}
    :entity/projectile-collision            {:create   cdq.entity.projectile-collision/create}
    :creature/stats                         {:create   cdq.entity.stats/create}
    :entity/fsm                             {:create!  cdq.entity.fsm/create!}
    :entity/inventory                       {:create!  cdq.entity.inventory/create!}
    :entity/skills                          {:create!  cdq.entity.skills/create!}})

(require 'cdq.effects)

(defn do! [ctx]
  (assoc ctx :ctx/entity-components (cdq.effects/walk-method-map method-mappings)))
