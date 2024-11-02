(ns moon.screen)

(defprotocol Screen
  (enter   [_])
  (exit    [_])
  (render  [_])
  (dispose [_]))

(declare ^:private screen-k
         ^:private screens)

(defn current []
  (and (bound? #'screen-k)
       (screen-k screens)))

(defn change
  "Calls `exit` on the current-screen and `enter` on the new screen."
  [new-k]
  (when-let [screen (current)]
    (exit screen))
  (let [screen (new-k screens)]
    (assert screen (str "Cannot find screen with key: " new-k))
    (bind-root #'screen-k new-k)
    (enter screen)))

(defn set-screens
  "Calls `change` to first "
  [screens]
  (bind-root #'screens screens)
  (change (ffirst screens)))

(defn dispose-all []
  (run! dispose (vals screens)))
