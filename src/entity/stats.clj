(ns entity.stats
  (:require [core.component :refer [defcomponent]]
            [core.data :as data]))

(defcomponent :stats/strength data/nat-int-attr)

(let [doc "action-time divided by this stat when a skill is being used.
          Default value 1.

          For example:
          attack/cast-speed 1.5 => (/ action-time 1.5) => 150% attackspeed."
      skill-speed-stat (assoc data/pos-attr :doc doc)]
  (defcomponent :stats/cast-speed   skill-speed-stat)
  (defcomponent :stats/attack-speed skill-speed-stat))

(defcomponent :stats/armor-save   {:widget :text-field :schema number?})
(defcomponent :stats/armor-pierce {:widget :text-field :schema number?})

(defcomponent :entity/stats (assoc (data/map-attribute :stats/strength
                                                       :stats/cast-speed
                                                       :stats/attack-speed
                                                       :stats/armor-save
                                                       :stats/armor-pierce
                                                       )
                              ; TODO also DRY @ modifier.all is default value 1 too...
                              :default-value {:stats/strength 1
                                              :stats/cast-speed 1
                                              :stats/attack-speed 1
                                              :stats/armor-save  0
                                              :stats/armor-pierce 0
                                              }
                              )) ; TODO default value missing... empty when created
