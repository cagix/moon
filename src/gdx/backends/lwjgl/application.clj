(ns gdx.backends.lwjgl.application
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn start! [[config {:keys [create! dispose! render! resize!]}]]
  (lwjgl/start-application! config
                            {:create! (fn [context]
                                        (let [[f params] create!]
                                          (f context params)))
                             :dispose! dispose!
                             :render! (fn []
                                        (let [[f params] render!]
                                          (f params)))
                             :resize! resize!}))
