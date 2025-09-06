(ns cdq.create.textures
  (:require [clojure.gdx :as gdx]
            [cdq.textures-impl]))

(defn do! [ctx]
  (assoc ctx :ctx/textures (cdq.textures-impl/create (gdx/files))))
