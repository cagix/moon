(ns gdl.create.app
  (:require [gdl.app :as app])
  (:import (com.badlogic.gdx Gdx)))

(defn do! [ctx]
  (assoc ctx :ctx/app (let [this Gdx/app]
                        (reify app/App
                          (post-runnable! [_ runnable]
                            (.postRunnable this runnable))))))
