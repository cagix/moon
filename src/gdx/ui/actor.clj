(ns gdx.ui.actor
  (:require [gdx.ui.utils :as utils])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Touchable)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils Align)
           (com.kotcrab.vis.ui.widget Tooltip
                                      VisLabel)
           (gdl.ui CtxStage)))

(defn get-stage-ctx [^Actor actor]
  (when-let [stage (.getStage actor)] ; for tooltip when actors are initialized w/o stage yet
    @(.ctx ^CtxStage stage)))

(defn get-x [^Actor actor]
  (.getX actor))

(defn get-y [^Actor actor]
  (.getY actor))

(defn get-name [^Actor actor]
  (.getName actor))

(defn user-object [^Actor actor]
  (.getUserObject actor))

(defn set-user-object! [^Actor actor object]
  (.setUserObject actor object))

(defn visible? [^Actor actor]
  (.isVisible actor))

(defn set-visible! [^Actor actor visible?]
  (.setVisible actor visible?))

(defn set-touchable! [^Actor actor touchable]
  (.setTouchable actor (case touchable
                         :disabled Touchable/disabled)))

(defn remove! [^Actor actor]
  (.remove actor))

(defn parent [^Actor actor]
  (.getParent actor))

(defn hit [^Actor actor [x y]]
  (let [v (.stageToLocalCoordinates actor (Vector2. x y))]
    (.hit actor (.x v) (.y v) true)))

(defn add-tooltip! [actor tooltip-text]
  (let [text? (string? tooltip-text)
        label (VisLabel. (if text? tooltip-text ""))
        tooltip (proxy [Tooltip] []
                  ; hooking into getWidth because at
                  ; https://github.com/kotcrab/vis-blob/master/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java#L271
                  ; when tooltip position gets calculated we setText (which calls pack) before that
                  ; so that the size is correct for the newly calculated text.
                  (getWidth []
                    (let [^Tooltip this this]
                      (when-not text?
                        (let [actor (.getTarget this)
                              ctx (get-stage-ctx actor)]
                          (when ctx ; ctx is only set later for update!/draw! ... not at starting of initialisation
                            (.setText this (str (tooltip-text ctx))))))
                      (proxy-super getWidth))))]
    (.setAlignment label Align/center)
    (.setTarget  tooltip actor)
    (.setContent tooltip label))
  actor)

(defn remove-tooltip! [actor]
  (Tooltip/removeTooltip actor))

(defn set-opts!
  [^Actor actor
   {:keys [id
           name
           user-object
           visible?
           center-position
           position] :as opts}]
  (when id
    (set-user-object! actor id))
  (when name
    (.setName actor name))
  (when user-object
    (set-user-object! actor user-object))
  (when (contains? opts :visible?)
    (set-visible! actor visible?))
  (when-let [[x y] center-position]
    (.setPosition actor
                  (- x (/ (.getWidth  actor) 2))
                  (- y (/ (.getHeight actor) 2))))
  (when-let [[x y] position]
    (.setPosition actor x y))
  (when-let [f (:click-listener opts)]
    (.addListener actor (utils/click-listener f)))
  (when-let [tooltip (:tooltip opts)]
    (add-tooltip! actor tooltip))
  (when-let [touchable (:actor/touchable opts)]
    (set-touchable! actor touchable))
  actor)

(defmulti construct :actor/type)

(defn construct? ^Actor [actor-declaration]
  (try
   (cond
    (instance? Actor actor-declaration) actor-declaration
    (map? actor-declaration) (construct actor-declaration)
    (nil? actor-declaration) nil
    :else (throw (ex-info "Cannot find constructor"
                          {:instance-actor? (instance? Actor actor-declaration)
                           :map? (map? actor-declaration)})))
   (catch Throwable t
     (throw (ex-info "Cannot create-actor"
                     {:actor-declaration actor-declaration}
                     t)))))
