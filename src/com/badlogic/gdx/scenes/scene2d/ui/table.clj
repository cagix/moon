(ns com.badlogic.gdx.scenes.scene2d.ui.table
  (:require [gdl.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.ui.cell :as cell])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Table)))

(defn- build? [actor-or-decl]
  (try (cond
        (instance? Actor actor-or-decl)
        actor-or-decl
        (nil? actor-or-decl)
        nil
        :else
        (scene2d/build actor-or-decl))
       (catch Throwable t
         (throw (ex-info ""
                         {:actor-or-decl actor-or-decl}
                         t)))))

(defn add! [^Table table actor-or-decl]
  (.add table ^Actor (build? actor-or-decl)))

(defn cells [^Table table]
  (.getCells table))

(defn add-rows! [^Table table rows]
  (doseq [row rows]
    (doseq [props-or-actor row]
      (cond
       (map? props-or-actor) (-> (add! table (:actor props-or-actor))
                                 (cell/set-opts! (dissoc props-or-actor :actor)))
       :else (add! table props-or-actor)))
    (.row table))
  table)
