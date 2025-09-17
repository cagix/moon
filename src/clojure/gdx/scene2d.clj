(ns clojure.gdx.scene2d
  (:require [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.ctx :as ctx]
            [clojure.scene2d.group :as group]
            [clojure.scene2d.stage :as stage]
            [clojure.scene2d.ui.table :as table]
            [clojure.scene2d.ui.widget :as widget]
            [com.badlogic.gdx.math.vector2 :as vector2]
            [com.badlogic.gdx.scenes.scene2d.ui.cell :as cell]
            [com.badlogic.gdx.scenes.scene2d.touchable :as touchable])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)
           (com.badlogic.gdx.scenes.scene2d.ui HorizontalGroup
                                               Stack
                                               Table
                                               Widget
                                               WidgetGroup)))

(extend-type Actor
  clojure.scene2d.actor/Actor
  (get-stage [actor]
    (.getStage actor))

  (get-x [actor]
    (.getX actor))

  (get-y [actor]
    (.getY actor))

  (get-name [actor]
    (.getName actor))

  (user-object [actor]
    (.getUserObject actor))

  (set-user-object! [actor object]
    (.setUserObject actor object))

  (visible? [actor]
    (.isVisible actor))

  (set-visible! [actor visible?]
    (.setVisible actor visible?))

  (set-touchable! [actor touchable]
    (.setTouchable actor (touchable/k->value touchable)))

  (remove! [actor]
    (.remove actor))

  (parent [actor]
    (.getParent actor))

  (stage->local-coordinates [actor position]
    (-> actor
        (.stageToLocalCoordinates (vector2/->java position))
        vector2/->clj))

  (hit [actor [x y]]
    (.hit actor x y true))

  (set-name! [actor name]
    (.setName actor name))

  (set-position! [actor x y]
    (.setPosition actor x y))

  (get-width [actor]
    (.getWidth actor))

  (get-height [actor]
    (.getHeight actor))

  (add-listener! [actor listener]
    (.addListener actor listener)))

(def ^:private opts-fn-map
  {:actor/name actor/set-name!
   :actor/user-object actor/set-user-object!
   :actor/visible?  actor/set-visible!
   :actor/touchable actor/set-touchable!
   :actor/listener actor/add-listener!
   :actor/position (fn [actor [x y]]
                     (actor/set-position! actor x y))
   :actor/center-position (fn [actor [x y]]
                            (actor/set-position! actor
                                                 (- x (/ (actor/get-width  actor) 2))
                                                 (- y (/ (actor/get-height actor) 2))))})

(defn actor-opts! [actor opts]
  (doseq [[k v] opts
          :let [f (get opts-fn-map k)]
          :when f]
    (f actor v))
  actor)

(defn- create*
  [{:keys [actor/act
           actor/draw]
    :as opts}]
  (doto (proxy [Actor] []
          (act [delta] ; TODO call proxy super required ?-> fixes tooltips in pure scene2d?
            (act this delta))
          (draw [batch parent-alpha]
            (draw this batch parent-alpha)))
    (actor-opts! opts)))

(defn- get-ctx [actor]
  (when-let [stage (actor/get-stage actor)]
    (stage/get-ctx stage)))

(defn- try-act [actor delta f]
  (when-let [ctx (get-ctx actor)]
    (f actor delta ctx)))

(defn- try-draw [actor f]
  (when-let [ctx (get-ctx actor)]
    (ctx/draw! ctx (f actor ctx))))

(defmethod scene2d/build :actor.type/actor
  [{:keys [act draw]
    :as opts}]
  (create*
   (assoc opts
          :actor/act (fn [actor delta]
                       (when act
                         (try-act actor delta act)))
          :actor/draw (fn [actor _batch _parent-alpha]
                        (when draw
                          (try-draw actor draw))))))

(defmethod scene2d/build :actor.type/widget
  [opts]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (when-let [f (:draw opts)]
        (try-draw this f)))))

(extend-type Widget
  clojure.scene2d.ui.widget/Widget
  (set-opts! [actor opts]
    (actor-opts! actor opts)))

(extend-type Group
  clojure.scene2d.group/Group
  (add! [group actor]
    (.addActor group actor))

  (find-actor [group name]
    (.findActor group name))

  (clear-children! [group]
    (.clearChildren group))

  (children [group]
    (.getChildren group)))

(defn group-opts! [group opts]
  (run! (fn [actor-or-decl]
          (group/add! group (if (instance? Actor actor-or-decl)
                              actor-or-decl
                              (scene2d/build actor-or-decl))))
        (:group/actors opts))
  (actor-opts! group opts))

(defmethod scene2d/build :actor.type/group [opts]
  (doto (Group.)
    (group-opts! opts)))

(defn set-widget-group-opts!
  [^WidgetGroup widget-group
   {:keys [fill-parent? pack?]
    :as opts}]
  (.setFillParent widget-group (boolean fill-parent?)) ; <- actor? TODO
  (when pack?
    (.pack widget-group))
  (group-opts! widget-group opts))

(comment
 ; fill parent & pack is from Widget TODO ( not widget-group ?)
 com.badlogic.gdx.scenes.scene2d.ui.Widget
 ; about .pack :
 ; Generally this method should not be called in an actor's constructor because it calls Layout.layout(), which means a subclass would have layout() called before the subclass' constructor. Instead, in constructors simply set the actor's size to Layout.getPrefWidth() and Layout.getPrefHeight(). This allows the actor to have a size at construction time for more convenient use with groups that do not layout their children.
 )

(defn- horizontal-group [{:keys [space pad] :as opts}]
  (let [group (HorizontalGroup.)]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    (group-opts! group opts)))

(defmethod scene2d/build :actor.type/horizontal-group [opts]
  (horizontal-group opts))

(defn- stack [opts]
  (doto (Stack.)
    (set-widget-group-opts! opts)))

(defmethod scene2d/build :actor.type/stack [opts]
  (stack opts))

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

(extend-type Table
  clojure.scene2d.ui.table/Table
  (add! [table actor-or-decl]
    (.add table ^Actor (build? actor-or-decl)))

  (cells [table]
    (.getCells table))

  (add-rows! [table rows]
    (doseq [row rows]
      (doseq [props-or-actor row]
        (cond
         (map? props-or-actor) (-> (table/add! table (:actor props-or-actor))
                                   (cell/set-opts! (dissoc props-or-actor :actor)))
         :else (table/add! table props-or-actor)))
      (.row table))
    table)

  (set-opts! [table {:keys [rows cell-defaults] :as opts}]
    (cell/set-opts! (.defaults table) cell-defaults)
    (doto table
      (table/add-rows! rows)
      (set-widget-group-opts! opts))))
