(ns dev.experiment
  (:require [gdl.app :refer [post-runnable]]
            [moon.component :as component]
            [moon.db :as db]
            [moon.player :as player]))

(comment

 (do
  (learn-skill! :skills/projectile)
  (learn-skill! :skills/spawn)
  (learn-skill! :skills/meditation)
  (learn-skill! :skills/death-ray)
  (learn-skill! :skills/convert)
  (learn-skill! :skills/blood-curse)
  (learn-skill! :skills/slow)
  (learn-skill! :skills/double-fireball))

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
