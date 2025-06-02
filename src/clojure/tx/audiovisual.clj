(ns clojure.tx.audiovisual
  (:require [clojure.ctx.effect-handler :refer [do!]]
            [clojure.db :as db]))

(defmethod do! :tx/audiovisual [[_ position audiovisual]
                                {:keys [ctx/db]
                                 :as ctx}]
  (let [{:keys [tx/sound
                entity/animation]} (if (keyword? audiovisual)
                                     (db/build db audiovisual ctx)
                                     audiovisual)]
    (do! [:tx/sound sound]
         ctx)
    (do! [:tx/spawn-effect
          position
          {:entity/animation animation
           :entity/delete-after-animation-stopped? true}]
         ctx)))
