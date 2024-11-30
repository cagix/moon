(ns ^:no-doc dev.experiment
  (:require [clojure.gdx :as gdx]
            [forge.db :as db]
            [moon.entity :as entity]
            [moon.world :as world :refer [player-eid]]))

(comment

 (do
  (learn-skill! :skills/projectile)
  (learn-skill! :skills/spawn)
  (learn-skill! :skills/meditation)
  (learn-skill! :skills/death-ray)
  (learn-skill! :skills/convert)
  (learn-skill! :skills/blood-curse)
  (learn-skill! :skills/slow)
  (learn-skill! :skills/bow)
  (learn-skill! :skills/double-fireball))

 ; Testing effect/target-entity
 ; stops when target not exists anymore
 ; or out of range

 ; 1. start application
 ; 2. start world
 ; 3. create creature
 (post-runnable (world/creature {:position [35 73]
                                 :creature-id :creatures/dragon-red
                                 :components {:entity/fsm {:fsm :fsms/npc
                                                           :initial-state :npc-sleeping}
                                              :entity/faction :evil}}))

 (learn-skill! :skills/bow) ; 1.5 seconds attacktime
 (post-tx! [:e/destroy (world/ids->eids 168)]) ; TODO how to get id ?
 ; check state is normal ... omg...

 ; start world - small empty test room
 ; 2 creatures - player?
 ; start skill w. applicable needs target (bow)
 ; this command:
 ; check skill has stopped using
 ; => this is basically a test for 'moon.entity.active'

 )

(defn- learn-skill! [skill-id]
  (post-runnable
   (swap! player-eid entity/add-skill (db/get skill-id))))

(defn- create-item! [item-id]
  (post-runnable
   (world/item (:position @player-eid) (db/get item-id))))

(defmacro post-runnable [& exprs]
  `(gdx/post-runnable (fn [] ~@exprs)))
