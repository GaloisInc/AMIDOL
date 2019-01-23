module Main exposing (main)

import Browser
import Draggable
import Draggable.Events exposing (onClick, onDragBy, onDragStart)
import Html exposing (Html)
import Html.Attributes
import Math.Vector2 as Vector2 exposing (Vec2, getX, getY)
import Svg exposing (Svg)
import Svg.Attributes as Attr
import Svg.Events exposing (onMouseUp)
import Svg.Keyed
import Svg.Lazy exposing (lazy)


main : Program () Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , subscriptions = subscriptions
        , view = view
        }


type alias Id =
    Int


type alias FileName =
    String


type alias Node =
    { id : Id
    , position : Vec2
    , clicked : Bool
    , image : FileName
    , label : String
    }


type alias Link =
    { source : Id
    , target : Id
    , label : String
    }


makeNode : Id -> ( Float, Float ) -> String -> String -> Node
makeNode id ( x, y ) svgfile label =
    { id = id
    , position = Vector2.vec2 x y
    , clicked = False
    , image = "img/" ++ svgfile
    , label = label
    }


dragNodeBy : Vec2 -> Node -> Node
dragNodeBy delta node =
    { node | position = node.position |> Vector2.add delta }


toggleClicked : Node -> Node
toggleClicked node =
    { node | clicked = not node.clicked }


type alias NodeGroup =
    { uid : Id
    , movingNode : Maybe Node
    , idleNodes : List Node
    }


allNodes : NodeGroup -> List Node
allNodes { movingNode, idleNodes } =
    movingNode
        |> Maybe.map (\a -> a :: idleNodes)
        |> Maybe.withDefault idleNodes


startDragging : Id -> NodeGroup -> NodeGroup
startDragging id ({ idleNodes, movingNode } as group) =
    let
        ( targetAsList, others ) =
            List.partition (.id >> (==) id) idleNodes
    in
    { group
        | idleNodes = others
        , movingNode = targetAsList |> List.head
    }


stopDragging : NodeGroup -> NodeGroup
stopDragging group =
    { group
        | idleNodes = allNodes group
        , movingNode = Nothing
    }


dragActiveBy : Vec2 -> NodeGroup -> NodeGroup
dragActiveBy delta group =
    { group | movingNode = group.movingNode |> Maybe.map (dragNodeBy delta) }


toggleNodeClicked : Id -> NodeGroup -> NodeGroup
toggleNodeClicked id group =
    let
        possiblyToggleNode node =
            if node.id == id then
                toggleClicked node

            else
                node
    in
    { group | idleNodes = group.idleNodes |> List.map possiblyToggleNode }


type alias Model =
    { nodes : NodeGroup
    , links : List Link
    , drag : Draggable.State Id
    }


type Msg
    = DragMsg (Draggable.Msg Id)
    | OnDragBy Vec2
    | StartDragging Id
    | ToggleNodeClicked Id
    | StopDragging


initialNodes : NodeGroup
initialNodes =
    NodeGroup 0
        Nothing
        [ makeNode 1 ( 10, 80 ) "susceptible.svg" "S"
        , makeNode 2 ( 110, 20 ) "infected.svg" "I"
        , makeNode 3 ( 210, 80 ) "recovered.svg" "R"
        ]


initialLinks : List Link
initialLinks =
    [ Link 1 2 "S --> I"
    , Link 2 3 "I --> R"
    ]


init : flags -> ( Model, Cmd Msg )
init _ =
    ( { nodes = initialNodes
      , links = initialLinks
      , drag = Draggable.init
      }
    , Cmd.none
    )


dragConfig : Draggable.Config Id Msg
dragConfig =
    Draggable.customConfig
        [ onDragBy (\( dx, dy ) -> Vector2.vec2 dx dy |> OnDragBy)
        , onDragStart StartDragging
        , onClick ToggleNodeClicked
        ]



-- UPDATE


update : Msg -> Model -> ( Model, Cmd Msg )
update msg ({ nodes } as model) =
    case msg of
        OnDragBy delta ->
            ( { model | nodes = nodes |> dragActiveBy delta }, Cmd.none )

        StartDragging id ->
            ( { model | nodes = nodes |> startDragging id }, Cmd.none )

        StopDragging ->
            ( { model | nodes = nodes |> stopDragging }, Cmd.none )

        ToggleNodeClicked id ->
            ( { model | nodes = nodes |> toggleNodeClicked id }, Cmd.none )

        DragMsg dragMsg ->
            Draggable.update dragConfig dragMsg model


subscriptions : Model -> Sub Msg
subscriptions { drag } =
    Draggable.subscriptions DragMsg drag



-- VIEW


nodeSize : Vec2
nodeSize =
    Vector2.vec2 50 50


view : Model -> Html Msg
view { nodes } =
    Html.div
        []
        [ Html.p
            [ Html.Attributes.style "padding-left" "8px" ]
            [ Html.text "Drag any icon around." ]
        , Svg.svg
            [ Attr.style "height: 100vh; width: 100vw; position: fixed;"
            ]
            [ background
            , nodesView nodes
            ]
        ]


nodesView : NodeGroup -> Svg Msg
nodesView nodeGroup =
    nodeGroup
        |> allNodes
        --|> List.reverse
        |> List.map nodeView
        |> Svg.node "g" []


nodeView : Node -> Svg Msg
nodeView { id, position, clicked, image, label } =
    Svg.image
        [ Attr.xlinkHref image
        , num Attr.width <| getX nodeSize
        , num Attr.height <| getY nodeSize
        , num Attr.x (getX position)
        , num Attr.y (getY position)
        , Attr.cursor "move"
        , Draggable.mouseTrigger id DragMsg
        , onMouseUp StopDragging
        ]
        []


background : Svg msg
background =
    Svg.rect
        [ Attr.x "0"
        , Attr.y "0"
        , Attr.width "100%"
        , Attr.height "100%"
        , Attr.fill "#eee"
        ]
        []


num : (String -> Svg.Attribute msg) -> Float -> Svg.Attribute msg
num attr value =
    attr (String.fromFloat value)
