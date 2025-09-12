(ns clojure.gdx.scene2d.actor.decl
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defmulti build :actor/type)

(defn build? [actor-declaration]
  (cond
   (instance? Actor actor-declaration) actor-declaration
   (map? actor-declaration) (build actor-declaration)
   (nil? actor-declaration) nil
   :else (throw (ex-info "Cannot find constructor"
                         {:instance-actor? (instance? Actor actor-declaration)
                          :map? (map? actor-declaration)}))))
