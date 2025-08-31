(ns cdq.dev.db.migrations.creature-stats
  (:require [cdq.ctx.db :as db]
            [cdq.utils :as utils]
            [clojure.java.io :as io]))

(def file "properties.edn")

(defn read-properties []
  (utils/io-slurp-edn file))

(def stat-keys
  [:entity/modifiers
   :entity/hp
   :entity/movement-speed
   :entity/aggro-range
   :entity/reaction-time
   :entity/mana
   :entity/strength
   :entity/cast-speed
   :entity/attack-speed
   :entity/armor-save
   :entity/armor-pierce])

(defn move-stats-in-separate-component [creature]
  (-> (apply dissoc creature stat-keys)
      (assoc :creature/stats (select-keys creature stat-keys))))

(def creature? :creature/level)

(defn update-properties-file! []
  (let [new-data (for [property (read-properties)]
                   (if (creature? property)
                     (move-stats-in-separate-component property)
                     property))]
    (db/save-vals! new-data
                   (io/resource file))))
(comment

 (count (filter creature? (read-properties)))
 (clojure.pprint/pprint (first (filter creature? (read-properties))))

 ; !!
 ;(require 'cdq.schemas-impl)

 (db/do! {} {:schemas "schema.edn"
             :properties "properties.edn"})

 (def example-creature {:entity/aggro-range 6,
                        :entity/animation
                        {:frame-duration 0.1,
                         :frames
                         [{:file "images/animations/toad-horned-1.png"}
                          {:file "images/animations/toad-horned-2.png"}
                          {:file "images/animations/toad-horned-3.png"}
                          {:file "images/animations/toad-horned-4.png"}],
                         :looping? true},
                        :creature/level 1,
                        :entity/reaction-time 12,
                        :entity/mana 11,
                        :property/pretty-name "Toad-horned",
                        :entity/strength 1,
                        :entity/species :species/toad,
                        :entity/body #:body{:flying? false, :height 11/24, :width 2/3},
                        :entity/movement-speed 1.6,
                        :entity/hp 12,
                        :property/id :creatures/toad-horned,
                        :entity/skills #{:skills/melee-attack}})

 (clojure.pprint/pprint
  (move-stats-in-separate-component example-creature))
 {:entity/animation
  {:frame-duration 0.1,
   :frames
   [{:file "images/animations/toad-horned-1.png"}
    {:file "images/animations/toad-horned-2.png"}
    {:file "images/animations/toad-horned-3.png"}
    {:file "images/animations/toad-horned-4.png"}],
   :looping? true},
  :creature/level 1,
  :property/pretty-name "Toad-horned",
  :entity/species :species/toad,
  :entity/body #:body{:flying? false, :height 11/24, :width 2/3},
  :creature/stats
  #:entity{:hp 12,
           :movement-speed 1.6,
           :aggro-range 6,
           :reaction-time 12,
           :mana 11,
           :strength 1},
  :property/id :creatures/toad-horned,
  :entity/skills #{:skills/melee-attack}}
 )
