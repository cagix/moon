(ns cdq.ui.actor
  (:require [cdq.graphics :as graphics])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            InputEvent
                                            Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Button
                                               Label
                                               Widget
                                               Window)
           (com.badlogic.gdx.scenes.scene2d.utils ClickListener)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils Align)
           (com.kotcrab.vis.ui.widget Tooltip
                                      VisLabel
                                      VisWindow)
           (cdq.ui CtxStage)))

(defn- click-listener [f]
  (proxy [ClickListener] []
    (clicked [event _x _y]
      (f @(.ctx ^CtxStage (InputEvent/.getStage event))))))

(defn- get-stage-ctx [^Actor actor]
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

(defn add-tooltip!
  "tooltip-text is a (fn [context]) or a string. If it is a function will be-recalculated every show.  Returns the actor."
  [actor tooltip-text]
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
    (.addListener actor (click-listener f)))
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

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(defn button?
  "Returns true if the actor or its parent is a button."
  [^Actor actor]
  (or (button-class? actor)
      (and (parent actor)
           (button-class? (parent actor)))))

; TODO buggy FIXME
(defn window-title-bar?
  "Returns true if the actor is a window title bar."
  [^Actor actor]
  (when (instance? Label actor)
    (when-let [p (parent actor)]
      (when-let [p (parent p)]
        (and (instance? VisWindow actor)
             (= (.getTitleLabel ^Window p) actor))))))

(defn find-ancestor-window ^Window [actor]
  (if-let [p (parent actor)]
    (if (instance? Window p)
      p
      (find-ancestor-window p))
    (throw (Error. (str "Actor has no parent window " actor)))))

(defn pack-ancestor-window! [actor]
  (.pack (find-ancestor-window actor)))

; actor was removed -> stage nil -> context nil -> error on text-buttons/etc.
(defn- try-act [actor delta f]
  (when-let [ctx (get-stage-ctx actor)]
    (f actor delta ctx)))

(defn- try-draw [actor f]
  (when-let [ctx (get-stage-ctx actor)]
    (graphics/handle-draws! (:ctx/graphics ctx)
                            (f actor ctx))))

(defmethod construct :actor.type/actor [opts]
  (doto (proxy [Actor] []
          (act [delta]
            (when-let [f (:act opts)]
              (try-act this delta f)))
          (draw [_batch _parent-alpha]
            (when-let [f (:draw opts)]
              (try-draw this f))))
    (set-opts! opts)))

(defmethod construct :actor.type/widget [opts]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (when-let [f (:draw opts)]
        (try-draw this f)))))

(defn toggle-visible! [actor]
  (set-visible! actor (not (visible? actor))))
