(ns cdq.start.lwjgl
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn do!
  [ctx]
  (lwjgl/start-application! ((requiring-resolve (::create-listener ctx)) ctx)
                            (::config2 ctx)))
