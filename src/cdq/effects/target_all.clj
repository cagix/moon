(ns cdq.effects.target-all
  (:require [cdq.world :as world]))

(defn affected-targets
  [active-entities
   world
   entity]
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(world/line-of-sight? world entity @%))
       (remove #(:entity/player? @%))))

(comment
 ; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
 ; same code as in render entities on world view screens/world
 ; TODO showing one a bit further up
 ; maybe world view port is cut
 ; not quite showing correctly.
 (let [targets (creatures-in-los-of-player)]
   (count targets)
   #_(sort-by #(% 1) (map #(vector (:entity.creature/name @%)
                                   (entity/position @%)) targets)))

 )
