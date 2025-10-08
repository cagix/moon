(ns cdq.game.create.get-gdx
  (:require [com.badlogic.gdx :as gdx]))

(defn do! [ctx]
  (assoc ctx :ctx/gdx (gdx/context)))
