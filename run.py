import os
import sys
import json
import argparse
import math
import platform

from subprocess import call
from pprint import pprint as pp


def produce_game_environment():

    sys.stdout.write("Compiling game engine..\n")

    if not os.path.exists("environment"):
        sys.stderr.write("Couldn't find the game engine source files!\n")
        sys.stderr.write("Make sure you have the environment/ folder in the current path.\n")
        exit(1)

    cmd = "cd ./environment; cmake .; make -j 4; cd ../; cp ./environment/halite ./halite"
    call([cmd], shell=True)

    if not os.path.exists("halite"):
        sys.stderr.write("Failed to produce executable environment\n")
        sys.stderr.write("Corrupt archive?")
        exit(1)


def prepare_env(args):

    produce_game_environment()

    makefile_set = {"makefile", "Makefile", "MAKEFILE"}

    if os.path.exists("CMakeLists.txt"):
        call(["cmake ."], shell=True)

    for makefile in makefile_set:
        if os.path.exists(makefile):
            sys.stdout.write("Compiling player sources..\n")
            if args.clean:
                call(["make clean"], shell=True)
            call(["make"], shell=True)

    call(["rm -rf *.hlt; "
          "rm -rf replays/*.hlt; "
          "rm -rf replays-readable/*.hlt; "
          "mkdir -p replays; "
          "mkdir -p replays-readable"], shell=True)


class HaliteEnv(object):

    def __init__(self,
                 player_bot_cmd,
                 height=30,
                 width=30,
                 seed=42,
                 max_turns=-1):

        self.bots      = [player_bot_cmd]
        self.height    = height
        self.width     = width
        self.seed      = seed
        self.max_turns = max_turns

    def __add_map(self, cmd):

        # Specify the map configuration
        cmd += "--height {} --width {} ".format(self.height, self.width)
        cmd += "-s {0}".format(self.seed)
        return cmd

    def __add_bot(self, cmd, bot_cmd):
        cmd += " \"{0}\" ".format(bot_cmd)
        return cmd

    def __add_bots(self, cmd):

        # Specify the number of bots and how to run them
        # cmd += " -n {} ".format(len(self.bots))
        for bot in self.bots:
            cmd = self.__add_bot(cmd, bot)
        return cmd

    def cleanup_old_replays(self, fname):

        if os.path.isfile(fname):
            os.unlink(fname)

        if os.path.isfile(fname + ".json"):
            os.unlink(fname + ".json")

    def run(self):
        sys.stdout.write("Map: Height {0}, Width {1}, Seed {2}\n".format(self.height, self.width, self.seed))

        cmd = "./halite --results-as-json "
        cmd = self.__add_map(cmd)

        fname = "./replay-{}-{}-{}".format(self.seed, self.width, self.height)
        self.cleanup_old_replays(fname)

        cmd = self.__add_bots(cmd)
        call([cmd], shell=True)

        binary_res, text_res = None, None

        if os.path.isfile(fname + ".hlt"):
            binary_res = fname + ".hlt"

        if os.path.isfile(fname + ".json"):
            text_res = fname + ".json"

        if not text_res:
            sys.stderr.write("There was an error during the game, "
                             "no valid replay file was produced!\n")
            return None, None

        return binary_res, text_res


def default_map_limit(height, width):
    return int(math.sqrt(height * width) * 10)


def compute_score(num_frames, soft_limit, hard_limit, game_weight):

    if num_frames <= soft_limit:
        return game_weight

    if num_frames >= hard_limit:
        return 0.0

    return game_weight * (1 - (num_frames - soft_limit) / (hard_limit - soft_limit))


def round_one(cmd, map):

    sys.stdout.write("Round 1 - single player challenge!\n")

    env   = HaliteEnv(cmd)
    games = [
        (32, 32, 42, 8000),
        (32, 32, 1673031865, 12000),
        (40, 40, 1773807367, 20000),
        (48, 48, 1942373999, 13000),
        (56, 56, 142342898, 12000)
    ]

    max_score    = 0.45                    # Round score
    game_weight  = max_score / len(games)  # Equal weight / game
    player_score = 0.0

    if map != -1:
        games = games[map:map + 1]

    game_scores = []

    for idx, game in enumerate(games):

        width, height, seed, expected_halite = game

        env.height = height
        env.width  = width
        env.seed   = seed
        points     = 0.0

        binary_log, text_log = env.run()

        if text_log is None:
            sys.stdout.write("Map {} score: {}\n".format(idx, points))
            continue

        else:
            with open(text_log, "r") as f:

                result = json.loads(f.read())

                halite_mined = result["stats"]["0"]["score"]

                sys.stdout.write("The objective was {} and your bot has collected {} halite.\n".format(expected_halite,
                                                                                                       halite_mined))
                if halite_mined >= expected_halite:
                    points = game_weight
                else:
                    points = game_weight * (halite_mined / expected_halite * 100) / 100
                    sys.stdout.write("You failed to mine sufficient resources!\n")

                sys.stdout.write("Map score: {}\n".format(points))
                game_scores.append(points)
                player_score += points

        call(["mv {} ./replays/replay-map-{}.hlt".format(binary_log, idx)], shell=True)
        call(["mv {} ./replays-readable/replay-map-{}.hlt".format(text_log, idx)], shell=True)

    final_score = round(min(player_score, max_score), 2)

    sys.stdout.write("Round 1 - done!\nFinal score: {}/{}\n".format(final_score,
                                                                    max_score))

    with open("result.json", "w") as f:
        result = {"final_score": final_score}
        json.dump(result, f)


def round_two(cmd, map):

    sys.stdout.write("Round 2 - 1vs1 battles!\n")

    env = HaliteEnv(cmd)

    games = [
        (32, 32, 20596, "./bots/Odysseus"),
        (32, 32, 75273, "./bots/Dragon"),
        (32, 32, 58900, "./bots/Joker"),
        (40, 40, 93689, "./bots/Odysseus"),
        (40, 40, 98091, "./bots/Dragon"),
        (56, 56, 1234, "./bots/Odysseus"),
        (56, 56, 42, "./bots/Odysseus"),
        (56, 56, 1024, "./bots/Dragon"),
    ]

    max_score = 0.45             # Round score
    game_weight = max_score / 5  # For max score you need to win on 5/8 maps
    player_score = 0.0
    battles_won = 0
    game_scores = []

    if map != -1:
        games = games[map:map + 1]

    for idx, game in enumerate(games):

        width, height, seed, bot = game
        env.bots = [cmd, bot]

        env.height = height
        env.width  = width
        env.seed   = seed
        points     = 0.0

        binary_log, text_log = env.run()

        if text_log is None:
            sys.stdout.write("Map {} score: {}\n".format(idx, points))
            continue
        else:

            with open(text_log, "r") as f:

                result = json.loads(f.read())

                if result["stats"]["0"]["rank"] == 1:
                    points = game_weight
                    sys.stdout.write("You've won!\n")
                elif result["stats"]["0"]["score"] >= 0.9 * result["stats"]["1"]["score"]:
                    points = game_weight * 0.9
                    sys.stdout.write("You've lost.. but you were very close!\n")
                else:
                    points = 0.0
                    sys.stdout.write("You've lost!\n")

                sys.stdout.write("Map score: {}\n".format(points))
                game_scores.append(points)
                player_score += points

        call(["mv {} ./replays/replay-map-{}.hlt".format(binary_log, idx)], shell=True)
        call(["mv {} ./replays-readable/replay-map-{}.hlt".format(text_log, idx)], shell=True)

    final_score = round(min(player_score, max_score), 2)

    sys.stdout.write("Round 2 - done!\nFinal score: {}/{}\n".format(final_score,
                                                                    max_score))

    with open("result.json", "w") as f:
        result = {"final_score": final_score}
        json.dump(result, f)

    return


def round_three(cmd, map):

    sys.stdout.write("Round 2 - 4 player battles!\n")
    sys.stdout.write("Coming soon!\n")
    return


def cleanup():
    call(["rm -f *.hlt; rm -rf replays/*.hlt; rm -rf replays-readable/*.hlt; rm -f *.log"], shell=True)
    if os.path.exists("makefile") or os.path.exists("Makefile"):
        call(["make clean"], shell=True)


def main():

    parser = argparse.ArgumentParser(description='PA project evaluator')
    parser.add_argument('--cmd', required=True, help="Command line instruction to execute the bot. eg: ./MyBot")
    parser.add_argument('--round', type=int, default=2, help="Round index (1, 2, or 3), default 2")
    parser.add_argument('--map', type=int, default=-1, help="Specify a specific map to play for the current round")
    parser.add_argument('--clean_logs', action="store_true", help="Remove logs/game results, call make clean")
    parser.add_argument('--clean', action="store_true", help="Call make clean before make when building player sources")

    args = parser.parse_args()
    prepare_env(args)

    rounds = [round_one, round_two, round_three]
    if args.round < 1 or args.round > len(rounds):
        sys.stderr.write("Invalid round parameter (should be an integer in [1, 3])\n")
        exit(1)

    # Let the games begin!
    rounds[args.round - 1](args.cmd, args.map)
    if args.clean_logs:
        cleanup()


if __name__ == "__main__":
    main()
