(ns app.components.editor.views
  (:require
   [app.utils.css.core :refer-macros [css]]
   [app.bindings.dnd-kit.core :as dnd]
   [uix.core :as uix :refer [$ defui]]
   [cuerdas.core :as str]
   [goog.math :as gmath]))

;; Styles ----------------------------------------------------------------------

(css wrapper-css []
  {:position "absolute"
   :inset 0
   :display "flex"
   :flex-direction "column"
   :align-items "center"
   :justify-content "center"
   :gap "10px"
   :background "oklch(10% 0.15 200)"
   "button" {:background "white"
             :color "black"
             :padding "var(--spacing-1-5) var(--spacing-2)"
             :border-radius "var(--spacing-1)"}})

(css video-css []
  {:display "block"
   :width "100%"
   :height "auto"
   :max-width "600px"})

(css video-wrapper-css []
  {:position "relative"})

(css cropper-css []
  {"--border-color" "oklch(0% 0 0 / 0.5)"
   :position "absolute"
   :inset 0
   :border "1px solid oklch(100% 0 0)"
   :box-shadow "0 0 0 1px var(--border-color), inset 0 0 0 1px var(--border-color)"})

(css cropper-handle-css [k]
  {:position "absolute"
   :z-index "1"
   :background "oklch(100% 0 0)"
   :box-shadow "0 0 0 1px var(--border-color)"
   :width "var(--size)"
   :aspect-ratio "1 / 1"
   :border-radius "99999px"})

(css cropper-bar-css []
  {:position "absolute"})

(css cropper-wrapper-css []
  {"--size" "10px"
   "--offset" "calc(var(--size) / 2)"
   "--border-color" "oklch(0% 0 0 / 0.5)"
   :position "absolute"
   :inset 0})

(css cropper-bar-border-css []
  {:position "absolute"
   :background "white"})

(css ffmpeg-command-css []
  {:background "white"
   :padding "10px"
   :font-family "monospace"
   :border-radius "5px"})

;; Component -------------------------------------------------------------------

(defn px [v]
  (str v "px"))

(defn translate-3D [axis]
  (str "translate3D(" (str/join "," axis) ")"))

(defn translate-x [dnd-opts pred]
  (let [value (get-in dnd-opts [:transform :x])]
    (when (pred value)
      (translate-3D [(str value "px") 0 0]))))

(defn translate-y [dnd-opts pred]
  (let [value (get-in dnd-opts [:transform :y])]
    (when (pred value)
      (translate-3D [0 (str value "px") 0]))))

(defui CropCircle [{:keys [direction offset]}]
  (let [bar-dnd (dnd/use-draggable direction)
        circle-dnd (dnd/use-draggable direction)
        {:keys [isDragging] :as dnd-opts} bar-dnd
        size-offset "calc(var(--offset) * -1)"
        center-offset "calc(50% - var(--offset))"]
    ($ :<>
       #_($ :div {:ref (:setNodeRef bar-dnd)
                  :class (cropper-bar-css)
                  :style (case direction
                           :top {:top size-offset
                                 :left 0
                                 :right 0
                                 :height "10px"
                                 :cursor "row-resize"
                                 :z-index 1}
                           :bottom {:bottom size-offset
                                    :left 0
                                    :right 0
                                    :height "10px"
                                    :cursor "row-resize"
                                    :z-index 1}
                           :right {:right size-offset
                                   :top 0
                                   :bottom 0
                                   :width "10px"
                                   :cursor "col-resize"
                                   :z-index 1}
                           :left {:left size-offset
                                  :top 0
                                  :bottom 0
                                  :width "10px"
                                  :cursor "col-resize"
                                  :z-index 1})
                  :on-pointer-down (get-in bar-dnd [:listeners :onPointerDown])})
       ($ :div
          {:ref (:setNodeRef circle-dnd)
           :style (case direction
                    :top    {:top size-offset
                             :left center-offset
                             :cursor "row-resize"}
                    :bottom {:bottom size-offset
                             :left center-offset
                             :cursor "row-resize"}
                    :right  {:top center-offset
                             :right size-offset
                             :cursor "col-resize"}
                    :left   {:left size-offset
                             :top center-offset
                             :cursor "col-resize"})
           :class (cropper-handle-css direction)
           :on-pointer-down (get-in circle-dnd [:listeners :onPointerDown])}))))


(defui CropRect [{:keys [ref offset children]}]
  ($ :div {:ref ref
           :class (cropper-css)
           :style {:top (px (:top offset))
                   :bottom (px (:bottom offset))
                   :right (px (:right offset))
                   :left (px (:left offset))}}

     ($ :div
        {:style {:position "absolute"
                 :width "1px"
                 :top 0
                 :bottom 0
                 :left "33%"
                 :background "oklch(100% 0 0 / 0.3)"}})
     ($ :div
        {:style {:position "absolute"
                 :width "1px"
                 :top 0
                 :bottom 0
                 :right "33%"
                 :background "oklch(100% 0 0 / 0.3)"}})
     ($ :div
        {:style {:position "absolute"
                 :height "1px"
                 :left 0
                 :right 0
                 :top "33%"
                 :background "oklch(100% 0 0 / 0.3)"}})
     ($ :div
        {:style {:position "absolute"
                 :height "1px"
                 :left 0
                 :right 0
                 :bottom "33%"
                 :background "oklch(100% 0 0 / 0.3)"}})
     children))

(defui Cropper [{:keys [resizer-ref offset]}]
  ($ :div {:class (cropper-wrapper-css)}
     ($ CropRect {:ref resizer-ref
                  :offset offset}
        (for [direction [:top :right :bottom :left]]
          ($ CropCircle {:key direction
                         :direction direction
                         :offset offset})))))

(defn ffmpeg-command [{:keys [offset file-name]}]
  (str/join " " ["ffmpeg" "-i" file-name "-vf" (str "\" " \")]))

(defn clamp
  "Takes a number and clamps it to within the provided bounds.
  Returns the input number if it is within bounds, or the nearest number within
  the bounds."
  [value min max]
  (gmath/clamp value min max))

(defui Editor []
  (let [[file-name set-file-name!] (uix/use-state nil)
        [video-dimensions set-video-dimensions!] (uix/use-state nil)
        [video-url set-video-url!] (uix/use-state nil)
        default-offset {:top 0
                        :bottom 0
                        :left 0
                        :right 0}
        [offset set-offset!] (uix/use-state default-offset)
        resizer-ref (uix/use-ref)
        video-ref (uix/use-ref)]
    (uix/use-effect
     (fn [] ()
       (when (and @video-ref video-url)

         (let [f (fn [_e]
                   (set-video-dimensions! {:width (.-videoWidth @video-ref)
                                           :height (.-videoHeight @video-ref)
                                           :element-width (.-clientWidth @video-ref)
                                           :element-height (.-clientHeight @video-ref)}))]
           (.addEventListener @video-ref "loadedmetadata" f)
           #(.removeEventListener @video-ref "loadedmetadata" f))))
     [video-url])
    ($ dnd/context
       {:on-drag-end (fn [opts]
                       (let [id (.. opts -active -id)
                             y (.. opts -delta -y)
                             x (.. opts -delta -x)
                             max-height (:element-height video-dimensions)
                             max-width (:element-width video-dimensions)
                             padding 10]
                         (set! (.. @resizer-ref -style -right) nil)
                         (cond-> offset
                           (= id :top) (assoc :top (gmath/clamp y 0 (+ max-height (:bottom offset) (- padding))))
                           (= id :bottom) (assoc :bottom (gmath/clamp y (+ (- max-height) (:top offset) padding) 0))
                           (= id :left) (assoc :left (gmath/clamp x 0 (+ max-width (:right offset) (- padding))))
                           (= id :right) (assoc :right (+ (- x) (- (:right offset))))
                           :always set-offset!)))
        :on-drag-move (fn [opts]
                        (let [id (.. opts -active -id)
                              max-height (:element-height video-dimensions)
                              max-width (:element-width video-dimensions)
                              y (.. opts -delta -y)
                              x (.. opts -delta -x)
                              padding 10]
                          (case id
                            :top (set! (.. @resizer-ref -style -top) (when (pos? y) (px y)))
                            :bottom (set! (.. @resizer-ref -style -bottom) (when (neg? y) (px (- y))))
                            :left (set! (.. @resizer-ref -style -left) (when (pos? x) (px x)))
                            :right (set! (.. @resizer-ref -style -right)
                                         (-> (+ (- x) (- (:right offset))) (px))))))}
       ($ :div {:class (wrapper-css)}
          ($ :button
             {:on-pointer-down #(set-offset! default-offset)}
             "Reset")
          ($ :input
             {:type "file"
              :accept "video/*"
              :on-change (fn [e]
                           (let [file (-> e .-target .-files (aget 0))
                                 url (js/URL.createObjectURL file)]
                             (set-file-name! (.-name file))
                             (set-video-url! url)))})
          (when video-url
            ($ :<>
               ($ :div {:class (video-wrapper-css)}
                  ($ Cropper {:resizer-ref resizer-ref
                              :offset offset})
                  ($ :video
                     {:class [(video-css)]
                      :ref video-ref
                      :src video-url}))
               (let [command (ffmpeg-command {:offset offset
                                              :file-name file-name})]
                 ($ :input {:class (ffmpeg-command-css)
                            :read-only true
                            :style {:width (str (count command) "ch")}
                            :value command
                            :on-click #(.select (.-target %))}))))))))
