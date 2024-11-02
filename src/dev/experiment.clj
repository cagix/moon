(ns dev.experiment
  (:require [gdl.app :refer [post-runnable]]
            [moon.component :as component]
            [moon.db :as db]
            [moon.player :as player]))

(comment

 ; * Test
 ; * if z-order/effect renders behind wall
 ; * => graphics txs?
 (post-tx! [:tx/line-render {:start [68 38]
                             :end [70 30]
                             :color [1 1 1]
                             :duration 2}])

 (do ; this only works in game screen otherwise action-bar uses wrong stage !!
     ; remove anyway other screens?! optionsmenu not needed -> menubar in dev mode
  (learn-skill! :skills/projectile)
  (learn-skill! :skills/spawn)
  (learn-skill! :skills/meditation)
  (learn-skill! :skills/death-ray)
  (learn-skill! :skills/convert)
  (learn-skill! :skills/blood-curse)
  (learn-skill! :skills/slow)
  (learn-skill! :skills/double-fireball))

 ; FIXME
 ; first says inventory is full
 ; ok! beholder doesn't have inventory - player entity needs inventory/...
 ; => tests...
 (create-item! :items/blood-glove)

 (require '[clojure.string :as str])
 (spit "item_tags.txt"
       (with-out-str
        (clojure.pprint/pprint
         (distinct
          (sort
           (mapcat
            (comp #(str/split % #"-")
                  name
                  :property/id)
            (db/all :properties/items)))))))

 )


(comment
 ; start world - small empty test room
 ; 2 creatures - player?
 ; start skill w. applicable needs target (bow)
 ; this command:
 (post-tx! [:e/destroy (entity/get-entity 68)])
 ; check skill has stopped using

 (post-tx! [:tx/creature {:position [35 73]
                          :creature-id :creatures/dragon-red
                          :components {:entity/fsm [:state/npc :npc-sleeping]
                                       :entity/faction :evil} }])
 )

(defn- post-tx! [tx]
  (post-runnable (component/->handle [tx])))

(defn- learn-skill! [skill-id]
  (post-tx!
   (fn []
     [[:tx/add-skill player/eid (db/get skill-id)]])))

(defn- create-item! [item-id]
  (post-tx!
   (fn []
     [[:tx/item (:position @player/eid) (db/get item-id)]])))
