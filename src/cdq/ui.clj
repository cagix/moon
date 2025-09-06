(ns cdq.ui
  (:require [clojure.gdx.scenes.scene2d :as scene2d])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defmulti create :actor/type)

(defn construct? ^Actor [actor-declaration]
  (try
   (cond
    (instance? Actor actor-declaration) actor-declaration
    (map? actor-declaration) (create actor-declaration)
    (nil? actor-declaration) nil
    :else (throw (ex-info "Cannot find constructor"
                          {:instance-actor? (instance? Actor actor-declaration)
                           :map? (map? actor-declaration)})))
   (catch Throwable t
     (throw (ex-info "Cannot create-actor"
                     {:actor-declaration actor-declaration}
                     t)))))

(defmethod create :actor.type/actor [opts]
  (scene2d/actor opts))
